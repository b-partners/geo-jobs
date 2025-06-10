package app.bpartners.geojobs.service;

import static net.sf.geographiclib.Geodesic.WGS84;

import java.util.function.Function;
import net.sf.geographiclib.PolygonArea;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

// Most ChatGPT generated
@Component
public class GeometrySquareMeterArea implements Function<Geometry, Double> {

  @Override
  public Double apply(Geometry geometry) {
    if (!(geometry instanceof Polygon || geometry instanceof MultiPolygon)) {
      throw new IllegalArgumentException(
          "Geometry must be Polygon or MultiPolygon, otherwise it is " + geometry);
    }

    PolygonArea polyArea = new PolygonArea(WGS84, false);

    if (geometry instanceof Polygon polygon) {
      addPolygon(polygon, polyArea);
    } else {
      MultiPolygon multiPolygon = (MultiPolygon) geometry;
      for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
        Polygon p = (Polygon) multiPolygon.getGeometryN(i);
        addPolygon(p, polyArea);
      }
    }

    // Retourne l’aire en m²
    return polyArea.Compute(false, false).area;
  }

  private void addPolygon(Polygon polygon, PolygonArea polyArea) {
    // Ajouter les points de l'enveloppe extérieure
    addRing(polygon.getExteriorRing(), polyArea);

    // Ajouter les trous éventuels (anneaux intérieurs)
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      addRing(polygon.getInteriorRingN(i), polyArea);
    }
  }

  private void addRing(LineString ring, PolygonArea polyArea) {
    Coordinate[] coords = ring.getCoordinates();
    for (Coordinate coord : coords) {
      polyArea.AddPoint(coord.y, coord.x); // Attention : GeographicLib attend (lat, lon)
    }
  }
}
