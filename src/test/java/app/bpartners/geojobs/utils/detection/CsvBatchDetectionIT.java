package app.bpartners.geojobs.utils.detection;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.GeoJsonOutput.ZIP;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.Point.TypeEnum.POINT;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.FINISHED;
import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manual batch tool (not a regular CI test).
 *
 * <p>Reads a CSV whose header is exactly :
 *
 * <pre>Type,Departement,Adresse,Ville,Code_postal,Position_GPS_Lat_long</pre>
 *
 * <p>and, for each row, in parallel :
 *
 * <ol>
 *   <li>builds a {@link CreateDetection} (only the dynamic fields {@code zoneName} and {@code
 *       geoJsonZone} are filled here ; static fields are filled in {@link
 *       #enrichWithStaticFields(CreateDetection)}),
 *   <li>launches a synchronous detection through {@code POST /detections/{id}/sync},
 *   <li>if the returned detection is on step {@code MACHINE_DETECTION} with status {@code
 *       FINISHED}/{@code SUCCEEDED}, creates a sub-folder named after the zone name and downloads
 *       {@code vggUrl} (as {@code <zoneName>.json}) and {@code imageUrl} (as {@code
 *       <zoneName>.jpg}),
 *   <li>then polls {@code GET /detections/{id}} for up to 5 minutes (every 30s) and, as soon as
 *       {@code geoJsonUrl} is present, downloads it (as {@code <zoneName>.geojson}) into the same
 *       folder.
 * </ol>
 *
 * <p>It runs against a <b>remote</b> Birdia server (the local IT app mocks the detectors and would
 * never produce real results), configured through environment variables :
 *
 * <pre>
 *   BIRDIA_BASE_URL      e.g. https://api.preprod.birdia.fr   (required)
 *   BIRDIA_API_KEY       your x-api-key                       (required)
 *   DETECTION_CSV_PATH   path to the CSV (default: src/test/resources/detection/addresses_sample.csv)
 *   DETECTION_OUTPUT_DIR output root     (default: build/detections, git-ignored)
 *   DETECTION_PARALLELISM fixed thread-pool size              (default: 8)
 * </pre>
 *
 * <p>Remove {@link Disabled} (or run with these env vars set) to execute it.
 */
@Disabled(
    "Manual batch tool: launches real detections against a remote Birdia server. "
        + "Set BIRDIA_BASE_URL / BIRDIA_API_KEY (+ optionally DETECTION_CSV_PATH) and remove "
        + "@Disabled to run.")
class CsvBatchDetectionIT {
  private static final Logger log = LoggerFactory.getLogger(CsvBatchDetectionIT.class);

  private static final String BASE_URL = System.getenv("BIRDIA_BASE_URL");
  private static final String API_KEY = System.getenv("BIRDIA_API_KEY");
  private static final Path CSV_PATH =
      Path.of(
          envOrDefault("DETECTION_CSV_PATH", "src/test/resources/detection/addresses_sample.csv"));
  private static final Path OUTPUT_ROOT =
      Path.of(envOrDefault("DETECTION_OUTPUT_DIR", "build/detections"));
  private static final int PARALLELISM =
      Integer.parseInt(envOrDefault("DETECTION_PARALLELISM", "8"));

  private static final Duration POLL_TIMEOUT = Duration.ofMinutes(5);
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(30);

  // shared, both are thread-safe (the generated ApiClient wraps a single java.net.http.HttpClient)
  private final HttpClient downloadClient = HttpClient.newHttpClient();
  private final DetectionApi detectionApi = newDetectionApi();

  @Test
  void process_csv_detections_in_parallel() throws Exception {
    assertNotNull(BASE_URL, "BIRDIA_BASE_URL env var is required");
    assertNotNull(API_KEY, "BIRDIA_API_KEY env var is required");

    var rows = readCsv(CSV_PATH);
    log.info("Loaded {} row(s) from {}", rows.size(), CSV_PATH.toAbsolutePath());
    Files.createDirectories(OUTPUT_ROOT);

    ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM);
    try {
      var futures =
          rows.stream()
              .map(row -> CompletableFuture.runAsync(() -> processRowSafely(row), executor))
              .toList();
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    } finally {
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.MINUTES);
    }
    log.info("Done. Results under {}", OUTPUT_ROOT.toAbsolutePath());
  }

  private void processRowSafely(CsvRow row) {
    var zoneName = zoneNameOf(row);
    try {
      processRow(row, zoneName);
    } catch (Exception e) {
      log.error("Detection failed for zone '{}'", zoneName, e);
    }
  }

  private void processRow(CsvRow row, String zoneName) throws Exception {
    var createDetection = toCreateDetection(row);
    enrichWithStaticFields(createDetection);

    var detectionId = randomUUID().toString();
    log.info("[{}] launching sync detection (id={})", zoneName, detectionId);
    var detection =
        detectionApi.processDetectionSynchronously(detectionId, toDebugMode(createDetection));

    var zoneDir = OUTPUT_ROOT.resolve(sanitize(zoneName));
    Files.createDirectories(zoneDir);

    if (isMachineDetectionSucceeded(detection)) {
      Path vggFile = null;
      Path imageFile = null;
      if (detection.getVggUrl() != null) {
        vggFile = zoneDir.resolve(sanitize(zoneName) + ".json");
        download(detection.getVggUrl(), vggFile);
      }
      if (detection.getImageUrl() != null) {
        imageFile = zoneDir.resolve(sanitize(zoneName) + ".jpg");
        download(detection.getImageUrl(), imageFile);
      }
      if (vggFile != null && imageFile != null) {
        drawAnnotatedImage(
            vggFile.toFile(),
            imageFile.toFile(),
            zoneDir.resolve(sanitize(zoneName) + "-annotated.png").toFile());
      }
    } else {
      log.warn("[{}] returned detection is not MACHINE_DETECTION FINISHED/SUCCEEDED", zoneName);
    }

    pollAndDownloadGeoJson(detectionId, zoneName, zoneDir);
  }

  /**
   * Polls {@code GET /detections/{id}} until {@code geoJsonUrl} is present or the timeout elapses.
   * Stops early if the detection health turns {@code FAILED}, since it will never produce a result.
   */
  private void pollAndDownloadGeoJson(String detectionId, String zoneName, Path zoneDir)
      throws Exception {
    var deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      var current = detectionApi.getProcessedDetection(detectionId);
      if (current.getGeoJsonUrl() != null) {
        download(current.getGeoJsonUrl(), zoneDir.resolve(sanitize(zoneName) + ".geojson"));
        return;
      }
      if (isFailed(current)) {
        log.warn("[{}] detection health is FAILED, stop polling for geoJsonUrl", zoneName);
        return;
      }
      TimeUnit.NANOSECONDS.sleep(Math.min(POLL_INTERVAL.toNanos(), deadline - System.nanoTime()));
    }
    log.warn("[{}] geoJsonUrl not available after {}", zoneName, POLL_TIMEOUT);
  }

  private static boolean isFailed(Detection detection) {
    var step = detection.getStep();
    return step != null
        && step.getStatus() != null
        && step.getStatus().getHealth() == Status.HealthEnum.FAILED;
  }

  // --------------------------------------------------------------------------
  // CSV row -> CreateDetection (dynamic fields only)
  // --------------------------------------------------------------------------

  /**
   * Builds the dynamic part of a {@link CreateDetection} from a CSV row :
   *
   * <ul>
   *   <li>{@code zoneName} = "Adresse Ville Code_postal" concatenated,
   *   <li>{@code geoJsonZone} = a single {@link Feature} holding a {@link Point} whose coordinates
   *       come from {@code Position_GPS_Lat_long}, <b>inverted to [long, lat]</b> as required by
   *       the GeoJSON spec (the CSV stores lat first).
   * </ul>
   */
  static CreateDetection toCreateDetection(CsvRow row) {
    var coordinates = row.gpsLatLong();
    var lat = coordinates.get(0);
    var lon = coordinates.get(1);

    var point = new Point().type(POINT).coordinates(List.of(lon, lat)); // [long, lat]
    var feature = new Feature().type(FEATURE).geometry(new FeatureGeometry(point));

    return new CreateDetection()
        .emailReceiver("tech@birdia.fr")
        .geoJsonOutput(ZIP)
        .needsImageOutput(true)
        .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
        .geoJsonDelimitationType(PARCEL_FREE_DELIMITATION)
        .zoneName(zoneNameOf(row))
        .geoJsonZone(List.of(feature));
  }

  /**
   * Hook for the static (non-dynamic) fields of the payload, to be filled by the caller, e.g. :
   *
   * <pre>
   *   createDetection
   *       .emailReceiver("...")
   *       .detectableObjectModelList(List.of(...))
   *       .geoJsonDelimitationType(...)
   *       .needsImageOutput(true);
   * </pre>
   */
  private void enrichWithStaticFields(CreateDetection createDetection) {
    // TODO fill in the static fields here.
  }

  private static String zoneNameOf(CsvRow row) {
    return String.join(
            " ",
            List.of(row.get("Adresse"), row.get("Ville"), row.get("Code_postal")).stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList())
        .trim();
  }

  /**
   * Copies every field of a {@link CreateDetection} onto the {@link CreateDetectionDebugMode} that
   * the sync endpoint expects.
   */
  private static CreateDetectionDebugMode toDebugMode(CreateDetection src) {
    return new CreateDetectionDebugMode()
        .debugMode(false)
        .emailReceiver(src.getEmailReceiver())
        .zoneName(src.getZoneName())
        .geoServerProperties(src.getGeoServerProperties())
        .detectableObjectModel(src.getDetectableObjectModel())
        .detectableObjectModelList(src.getDetectableObjectModelList())
        .geoJsonZone(src.getGeoJsonZone())
        .geoJsonDelimitationType(src.getGeoJsonDelimitationType())
        .geoJsonOutput(src.getGeoJsonOutput())
        .needsImageOutput(src.getNeedsImageOutput())
        .toNotify(src.getToNotify());
  }

  private static boolean isMachineDetectionSucceeded(Detection detection) {
    var step = detection.getStep();
    return step != null
        && step.getName() == MACHINE_DETECTION
        && step.getStatus() != null
        && step.getStatus().getProgression() == FINISHED
        && step.getStatus().getHealth() == SUCCEEDED;
  }

  // --------------------------------------------------------------------------
  // VGG annotation (adapted from VggPolygonDrawer)
  // --------------------------------------------------------------------------

  /**
   * Draws the VGG polygons on the downloaded image and writes the annotated PNG to {@code output}.
   */
  private void drawAnnotatedImage(File vggFile, File imageFile, File output) throws IOException {
    BufferedImage image = ImageIO.read(imageFile);
    Graphics2D g2d = image.createGraphics();
    g2d.setStroke(new BasicStroke(4));
    g2d.setFont(new Font("Arial", Font.PLAIN, 36));

    JsonNode root = new ObjectMapper().readTree(vggFile);
    if (!root.isArray()) {
      throw new IllegalArgumentException("VGG file must contain a list of elements: " + vggFile);
    }

    int addressCount = 0; // to vertically space repeated addresses
    for (JsonNode element : root) {
      Iterator<Map.Entry<String, JsonNode>> uuidEntries = element.fields();
      while (uuidEntries.hasNext()) {
        JsonNode vggData = uuidEntries.next().getValue();
        JsonNode properties = vggData.get("properties");
        JsonNode regions = vggData.get("regions");

        String firstAddress = null;
        if (properties != null
            && properties.has("addresses")
            && properties.get("addresses").isArray()
            && properties.get("addresses").size() > 0) {
          firstAddress = properties.get("addresses").get(0).asText();
        }

        int firstPolygonX = -1;
        int firstPolygonY = -1;
        if (regions != null && regions.isObject()) {
          Iterator<Map.Entry<String, JsonNode>> regionEntries = regions.fields();
          while (regionEntries.hasNext()) {
            JsonNode regionNode = regionEntries.next().getValue();
            JsonNode shape = regionNode.get("shape_attributes");
            JsonNode region = regionNode.get("region_attributes");
            if (shape == null || !"Polygon".equalsIgnoreCase(shape.get("name").asText())) {
              continue;
            }

            JsonNode xPointsNode = shape.get("all_points_x");
            JsonNode yPointsNode = shape.get("all_points_y");
            int numPoints = xPointsNode.size();
            int[] xPoints = new int[numPoints];
            int[] yPoints = new int[numPoints];
            for (int i = 0; i < numPoints; i++) {
              xPoints[i] = (int) Math.round(xPointsNode.get(i).asDouble());
              yPoints[i] = (int) Math.round(yPointsNode.get(i).asDouble());
            }

            String label =
                region != null && region.has("label") ? region.get("label").asText() : null;
            var color = getColorFromDetectedType(parseDetectableType(label));
            g2d.setColor(Color.decode(color));
            g2d.drawPolygon(new Polygon(xPoints, yPoints, numPoints));

            if (firstPolygonX == -1 && firstPolygonY == -1) {
              firstPolygonX = Arrays.stream(xPoints).min().orElse(0);
              firstPolygonY = Arrays.stream(yPoints).min().orElse(0) - 15;
            }
          }

          if (firstAddress != null
              && !firstAddress.isBlank()
              && firstPolygonX >= 0
              && firstPolygonY >= 0) {
            g2d.setColor(Color.BLUE);
            g2d.drawString(firstAddress, firstPolygonX, firstPolygonY + (addressCount * 40));
            addressCount++;
          }
        }
      }
    }

    g2d.dispose();
    ImageIO.write(image, "png", output);
    log.info("annotated image written -> {}", output);
  }

  private static DetectableType parseDetectableType(String label) {
    if (label == null) {
      return null;
    }
    try {
      return DetectableType.valueOf(label.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null; // unknown label -> default (grey) color
    }
  }

  private static String getColorFromDetectedType(DetectableType detectableType) {
    if (detectableType == null) {
      return "#8C8B89"; // gris
    }
    return switch (detectableType) {

      // 🌿 Espaces verts & arbres
      case ARBRE, ESPACE_VERT, ESPACE_VERT_PARKING -> "#4CAF50"; // vert

      // 🍄 Moisissures
      case MOISISSURE, MOISISSURE_CLAIR, MOISISSURE_COULEUR, MOISISSURE_NOIRCIE ->
          "#795548"; // Marron

      // 💧 Humidité
      case HUMIDITE, HUMIDITE_CLAIR, HUMIDITE_INTENSE -> "#2196F3"; // bleu

      // ⚠️ Usure
      case USURE, USURE_IMPORTANTE, USURE_LEGER -> "#F44336"; // rouge

      // Obstacle
      case OBSTACLE, VELUX, CHEMINEE -> "#000000"; // noir

      case TOITURE_REVETEMENT -> "#db531d"; // marron

      // ⬜ Tout le reste
      default -> "#8C8B89"; // gris
    };
  }

  // --------------------------------------------------------------------------
  // infra : api client, downloads, CSV parsing
  // --------------------------------------------------------------------------

  private static DetectionApi newDetectionApi() {
    var client = new ApiClient();
    if (BASE_URL != null) {
      var uri = URI.create(BASE_URL);
      client.setScheme(uri.getScheme());
      client.setHost(uri.getHost());
      client.setPort(uri.getPort()); // -1 when omitted -> default port for the scheme
      if (uri.getPath() != null && !uri.getPath().isBlank()) {
        client.setBasePath(uri.getPath());
      }
    }
    client.setRequestInterceptor(builder -> builder.header(API_KEY_HEADER, API_KEY));
    return new DetectionApi(client);
  }

  private void download(String url, Path target) throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    var response = downloadClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() / 100 != 2) {
      throw new IOException("Download failed (" + response.statusCode() + ") for " + url);
    }
    try (var in = response.body()) {
      Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    log.info("downloaded {} -> {}", url, target);
  }

  private static List<CsvRow> readCsv(Path path) throws IOException {
    var lines = Files.readAllLines(path, UTF_8);
    if (lines.isEmpty()) {
      return List.of();
    }
    var headers = parseCsvLine(lines.get(0));
    var rows = new ArrayList<CsvRow>();
    for (int i = 1; i < lines.size(); i++) {
      var line = lines.get(i);
      if (line.isBlank()) {
        continue;
      }
      var values = parseCsvLine(line);
      var byHeader = new LinkedHashMap<String, String>();
      for (int c = 0; c < headers.size(); c++) {
        // if the (unquoted) GPS column embedded a comma, the surplus fields belong to the last
        // column
        if (c == headers.size() - 1 && values.size() > headers.size()) {
          byHeader.put(headers.get(c), String.join(",", values.subList(c, values.size())));
        } else {
          byHeader.put(headers.get(c), c < values.size() ? values.get(c) : "");
        }
      }
      rows.add(new CsvRow(byHeader));
    }
    return rows;
  }

  /**
   * Minimal RFC4180-ish parser: handles double-quoted fields with embedded commas and "" escapes.
   */
  private static List<String> parseCsvLine(String line) {
    var fields = new ArrayList<String>();
    var current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (inQuotes) {
        if (ch == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(ch);
        }
      } else if (ch == '"') {
        inQuotes = true;
      } else if (ch == ',') {
        fields.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(ch);
      }
    }
    fields.add(current.toString().trim());
    return fields;
  }

  private static String sanitize(String name) {
    return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  private static String envOrDefault(String key, String defaultValue) {
    var value = System.getenv(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }

  /** One CSV row, keyed by header name. */
  record CsvRow(Map<String, String> byHeader) {
    String get(String header) {
      return byHeader.get(header);
    }

    /** Parses {@code Position_GPS_Lat_long} into {@code [lat, long]} (as stored in the CSV). */
    List<BigDecimal> gpsLatLong() {
      var raw = get("Position_GPS_Lat_long");
      if (raw == null || raw.isBlank()) {
        throw new IllegalArgumentException("Missing Position_GPS_Lat_long for row " + byHeader);
      }
      var parts = raw.trim().split(raw.contains(",") ? "," : "\\s+");
      if (parts.length < 2) {
        throw new IllegalArgumentException("Cannot parse GPS '" + raw + "' for row " + byHeader);
      }
      return List.of(new BigDecimal(parts[0].trim()), new BigDecimal(parts[1].trim()));
    }
  }
}
