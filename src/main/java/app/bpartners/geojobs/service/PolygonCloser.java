package app.bpartners.geojobs.service;

import java.util.function.Function;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.stereotype.Component;

@Component
public class PolygonCloser implements Function<Polygon, Polygon> {

  private final GeometryFactory geometryFactory = new GeometryFactory();

  @Override
  public Polygon apply(Polygon polygon) {
    if (polygon == null) return null;

    LinearRing shell = closeRing((LinearRing) polygon.getExteriorRing());
    LinearRing[] holes = new LinearRing[polygon.getNumInteriorRing()];

    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      holes[i] = closeRing((LinearRing) polygon.getInteriorRingN(i));
    }

    Polygon closedPolygon = geometryFactory.createPolygon(shell, holes);

    // Correction automatique si topologie invalide
    if (!closedPolygon.isValid()) {
      closedPolygon = (Polygon) GeometryFixer.fix(closedPolygon);
    }

    return closedPolygon;
  }

  private LinearRing closeRing(LinearRing ring) {
    Coordinate[] coords = ring.getCoordinates();
    if (coords.length > 0 && !coords[0].equals2D(coords[coords.length - 1])) {
      Coordinate[] closed = new Coordinate[coords.length + 1];
      System.arraycopy(coords, 0, closed, 0, coords.length);
      closed[coords.length] = coords[0];
      coords = closed;
    }
    return geometryFactory.createLinearRing(coords);
  }
}
