package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import java.util.function.Function;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

@Component
public class PolygonCloser implements Function<Polygon, Polygon> {

  @Override
  public Polygon apply(Polygon polygon) {
    if (polygon == null) {
      return null;
    }

    // Fermer l'anneau extérieur
    LinearRing shell = closeRing((LinearRing) polygon.getExteriorRing());

    // Fermer les trous s'il y en a
    int numHoles = polygon.getNumInteriorRing();
    LinearRing[] holes = new LinearRing[numHoles];
    for (int i = 0; i < numHoles; i++) {
      holes[i] = closeRing((LinearRing) polygon.getInteriorRingN(i));
    }

    return geometryFactory.createPolygon(shell, holes);
  }

  private LinearRing closeRing(LinearRing ring) {
    Coordinate[] coords = ring.getCoordinates();

    // Si le ring est déjà fermé, inutile de le fermer
    if (coords.length < 1 || coords[0].equals2D(coords[coords.length - 1])) {
      return geometryFactory.createLinearRing(coords);
    }

    // Fermer en ajoutant la première coordonnée à la fin
    Coordinate[] closedCoords = new Coordinate[coords.length + 1];
    System.arraycopy(coords, 0, closedCoords, 0, coords.length);
    closedCoords[closedCoords.length - 1] = coords[0];

    return geometryFactory.createLinearRing(closedCoords);
  }
}
