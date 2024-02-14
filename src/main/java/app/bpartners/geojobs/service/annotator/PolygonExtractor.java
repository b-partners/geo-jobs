package app.bpartners.geojobs.service.annotator;

import app.bpartners.geojobs.repository.annotator.gen.Point;
import app.bpartners.geojobs.repository.annotator.gen.Polygon;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class PolygonExtractor implements Function<DetectedObject, Polygon> {
  @Override
  public Polygon apply(DetectedObject detectedObject) {
    return detectedObject.getFeature().getGeometry().getCoordinates().stream()
        .map(
            multipolygonCoordinates ->
                Polygon.builder()
                    .points(extractMultipolygonPoints(multipolygonCoordinates).get(0))
                    .build())
        .toList()
        .get(0);
  }

  private static List<List<Point>> extractMultipolygonPoints(
      List<List<List<BigDecimal>>> multipolygonCoordinates) {
    return multipolygonCoordinates.stream()
        .map(PolygonExtractor::extractPolygonCoordinates)
        .toList();
  }

  private static List<Point> extractPolygonCoordinates(List<List<BigDecimal>> polygonCoordinates) {
    return polygonCoordinates.stream().map(PolygonExtractor::extractPoint).toList();
  }

  private static Point extractPoint(List<BigDecimal> cor) {
    return Point.builder().x(cor.get(0).doubleValue()).y(cor.get(1).doubleValue()).build();
  }
}
