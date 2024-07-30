package app.bpartners.geojobs.service.annotator;

import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PolygonExtractor implements Function<DetectedObject, Polygon> {
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
    return new Point().x(cor.get(0).doubleValue()).y(cor.get(1).doubleValue());
  }

  @Override
  public Polygon apply(DetectedObject machineDetectedObject) {
    return machineDetectedObject.getFeature().getGeometry().getCoordinates().stream()
        .map(
            multipolygonCoordinates ->
                new Polygon().points(extractMultipolygonPoints(multipolygonCoordinates).getFirst()))
        .toList()
        .getFirst();
  }
}
