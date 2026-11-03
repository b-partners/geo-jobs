package app.bpartners.geojobs.service.dashboard;

import app.bpartners.geojobs.repository.model.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class AnalysisProjectKeys {
  private static final String VERSION_PREFIX = "v1:";
  private static final int COORDINATE_SCALE = 6;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Comparator<BigDecimal[]> POINT_ORDER =
      Comparator.<BigDecimal[], BigDecimal>comparing(point -> point[0])
          .thenComparing(point -> point[1]);

  private AnalysisProjectKeys() {}

  public static Optional<String> of(List<Feature> features) {
    if (features == null) {
      return Optional.empty();
    }
    var canonicalParts =
        features.stream()
            .filter(Objects::nonNull)
            .map(Feature::getGeometry)
            .filter(Objects::nonNull)
            .flatMap(geometry -> canonicalGeometry(geometry).stream())
            .sorted()
            .toList();
    if (canonicalParts.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(VERSION_PREFIX + sha256(String.join("|", canonicalParts)));
  }

  public static boolean isIndividual(List<Feature> features) {
    return features != null
        && features.stream()
                .filter(Objects::nonNull)
                .filter(feature -> feature.getGeometry() != null)
                .count()
            == 1;
  }

  public static boolean hasGeometry(List<Feature> features) {
    return features != null
        && features.stream()
            .filter(Objects::nonNull)
            .anyMatch(feature -> feature.getGeometry() != null);
  }

  public static Optional<String> ofIndividual(List<Feature> features) {
    return isIndividual(features) ? of(features) : Optional.empty();
  }

  private static List<String> canonicalGeometry(Feature.FeatureGeometry geometry) {
    var raw = geometry.getActualInstanceStringValue();
    if (raw == null) {
      return List.of();
    }
    JsonNode root;
    try {
      root = OBJECT_MAPPER.readTree(raw);
    } catch (Exception e) {
      return List.of();
    }
    var coordinates = root.path("coordinates");
    var type = root.path("type").asText("").replace("_", "").toLowerCase();
    return switch (type) {
      case "point" -> List.of("P" + canonicalPoint(coordinates));
      case "polygon" -> canonicalPolygon(coordinates);
      case "multipolygon" ->
          children(coordinates).flatMap(polygon -> canonicalPolygon(polygon).stream()).toList();
      default -> List.of();
    };
  }

  private static List<String> canonicalPolygon(JsonNode rings) {
    return children(rings).map(AnalysisProjectKeys::canonicalRing).toList();
  }

  private static String canonicalRing(JsonNode ring) {
    var points = new ArrayList<>(children(ring).map(AnalysisProjectKeys::roundedPoint).toList());
    if (points.size() > 1 && samePoint(points.getFirst(), points.getLast())) {
      points.removeLast();
    }
    if (signedArea(points) < 0) {
      Collections.reverse(points);
    }
    var start = points.indexOf(points.stream().min(POINT_ORDER).orElseThrow());
    Collections.rotate(points, -start);
    return "R" + points.stream().map(AnalysisProjectKeys::format).toList();
  }

  private static String canonicalPoint(JsonNode point) {
    return format(roundedPoint(point));
  }

  private static BigDecimal[] roundedPoint(JsonNode point) {
    return new BigDecimal[] {round(point.path(0)), round(point.path(1))};
  }

  private static BigDecimal round(JsonNode value) {
    return value.decimalValue().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
  }

  private static boolean samePoint(BigDecimal[] first, BigDecimal[] second) {
    return POINT_ORDER.compare(first, second) == 0;
  }

  private static double signedArea(List<BigDecimal[]> points) {
    var area = 0.0;
    for (int i = 0; i < points.size(); i++) {
      var current = points.get(i);
      var next = points.get((i + 1) % points.size());
      area +=
          current[0].doubleValue() * next[1].doubleValue()
              - next[0].doubleValue() * current[1].doubleValue();
    }
    return area / 2;
  }

  private static String format(BigDecimal[] point) {
    return point[0].toPlainString() + " " + point[1].toPlainString();
  }

  private static Stream<JsonNode> children(JsonNode node) {
    return StreamSupport.stream(node.spliterator(), false);
  }

  private static String sha256(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
