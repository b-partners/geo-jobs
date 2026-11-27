package app.bpartners.geojobs.service.tiling.downloader;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.zip.FileUnzipper;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.ParcelContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class HttpApiTilesDownloaderTest {
  @TempDir Path tempDir;

  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  FileUnzipper fileUnzipperMock = mock();
  HttpServer server;
  String baseUrl;
  AtomicReference<String> tilerQuery = new AtomicReference<>();
  AtomicReference<String> tilerBody = new AtomicReference<>();
  AtomicReference<String> tilerContentType = new AtomicReference<>();
  HttpApiTilesDownloader subject;

  @BeforeEach
  void setup() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.start();
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    subject =
        new HttpApiTilesDownloader(
            baseUrl,
            objectMapper,
            new FileWriter(objectMapper, new ExtensionGuesser()),
            fileUnzipperMock);
  }

  @AfterEach
  void cleanup() {
    server.stop(0);
  }

  private static byte[] zipWithOneEntry() throws IOException {
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry("layer/20/1/1.jpg"));
      zip.write(new byte[] {1, 2, 3});
      zip.closeEntry();
    }
    return bytes.toByteArray();
  }

  private void tilerRespondsWith(int status, String body) {
    server.createContext(
        "/geo-tiles",
        exchange -> {
          tilerQuery.set(exchange.getRequestURI().getQuery());
          tilerContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
          tilerBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
          reply(exchange, status, body.getBytes(StandardCharsets.UTF_8));
        });
  }

  private void zipServedAt(String path, byte[] zip) {
    server.createContext(path, exchange -> reply(exchange, 200, zip));
  }

  private static void reply(HttpExchange exchange, int status, byte[] body) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
    if (body.length > 0) {
      exchange.getResponseBody().write(body);
    }
    exchange.close();
  }

  private ParcelContent parcelContent(Map<String, Object> featureProperties) throws IOException {
    var feature =
        Feature.builder()
            .zoom(20)
            .properties(new HashMap<>(featureProperties))
            .geometry(
                Feature.FeatureGeometry.builder()
                    .geometryType(POLYGON)
                    .actualInstanceStringValue(
                        "{\"type\":\"Polygon\",\"coordinates\":[[[2,48],[3,48],[3,49],[2,48]]]}")
                    .build())
            .build();
    return ParcelContent.builder()
        .id("parcelId")
        .feature(feature)
        .geoServerUrl(new URL("http://geoserver.test/wms"))
        .geoServerParameter(
            new GeoServerParameter()
                .service("WMS")
                .request("GetMap")
                .layers("defaultLayer")
                .styles("")
                .format("image/jpeg")
                .transparent(false)
                .version("1.1.1")
                .width(1024)
                .height(1024)
                .srs("EPSG:3857"))
        .build();
  }

  @Test
  void downloads_tiles_zip_and_unzips_it_in_the_default_layer_directory() throws Exception {
    var unzipped = Files.createDirectory(tempDir.resolve("unzipped"));
    tilerRespondsWith(200, "{\"file_url\":\"" + baseUrl + "/files/tiles.zip\"}");
    zipServedAt("/files/tiles.zip", zipWithOneEntry());
    when(fileUnzipperMock.apply(any(ZipFile.class), eq("defaultLayer"))).thenReturn(unzipped);

    var actual = subject.apply(parcelContent(Map.of()));

    assertEquals(unzipped.toFile(), actual);
    assertEquals("zoom_size=20", tilerQuery.get());
    assertTrue(tilerContentType.get().startsWith("multipart/form-data"));
    assertTrue(tilerBody.get().contains("\"url\":\"http://geoserver.test/wms\""));
    assertTrue(tilerBody.get().contains("\"layers\":\"defaultLayer\""));
    assertTrue(tilerBody.get().contains("\"srs\":\"EPSG:3857\""));
    assertTrue(tilerBody.get().contains("\"concurrency\":1"));
    assertTrue(tilerBody.get().contains("FeatureCollection"));
    assertTrue(tilerBody.get().contains("Polygon"));
    verify(fileUnzipperMock).apply(any(ZipFile.class), eq("defaultLayer"));
  }

  @Test
  void priority_layer_of_the_feature_replaces_the_default_layer() throws Exception {
    var unzipped = Files.createDirectory(tempDir.resolve("unzipped"));
    tilerRespondsWith(200, "{\"file_url\":\"" + baseUrl + "/files/tiles.zip\"}");
    zipServedAt("/files/tiles.zip", zipWithOneEntry());
    when(fileUnzipperMock.apply(any(ZipFile.class), eq("priorityLayer"))).thenReturn(unzipped);

    var actual = subject.apply(parcelContent(Map.of("priorityLayer", "priorityLayer")));

    assertEquals(unzipped.toFile(), actual);
    assertTrue(tilerBody.get().contains("\"layers\":\"priorityLayer\""));
    var layerCaptor = ArgumentCaptor.forClass(String.class);
    verify(fileUnzipperMock).apply(any(ZipFile.class), layerCaptor.capture());
    assertEquals("priorityLayer", layerCaptor.getValue());
  }

  @Test
  void tiler_error_is_reported_as_a_server_exception() throws Exception {
    tilerRespondsWith(500, "{\"error\":\"tiler exploded\"}");
    var parcelContent = parcelContent(Map.of());

    var error = assertThrows(ApiException.class, () -> subject.apply(parcelContent));

    assertEquals(ApiException.ExceptionType.SERVER_EXCEPTION, error.getType());
  }

  @Test
  void tiler_answer_without_body_is_reported_as_a_server_error() throws Exception {
    tilerRespondsWith(200, "");
    var parcelContent = parcelContent(Map.of());

    var error = assertThrows(ApiException.class, () -> subject.apply(parcelContent));

    assertEquals("Server error", error.getMessage());
  }

  @Test
  void invalid_zip_url_given_by_the_tiler_is_reported() throws Exception {
    tilerRespondsWith(200, "{\"file_url\":\"http://bad host/tiles.zip\"}");
    var parcelContent = parcelContent(Map.of());

    var error = assertThrows(RuntimeException.class, () -> subject.apply(parcelContent));

    assertTrue(error.getCause() instanceof java.net.URISyntaxException);
  }

  @Test
  void downloaded_file_that_is_not_a_zip_is_reported() throws Exception {
    tilerRespondsWith(200, "{\"file_url\":\"" + baseUrl + "/files/tiles.zip\"}");
    zipServedAt("/files/tiles.zip", "this is not a zip archive".getBytes(StandardCharsets.UTF_8));
    var parcelContent = parcelContent(Map.of());

    var error = assertThrows(RuntimeException.class, () -> subject.apply(parcelContent));

    assertTrue(error.getCause() instanceof IOException);
  }
}
