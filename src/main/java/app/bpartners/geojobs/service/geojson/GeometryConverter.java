package app.bpartners.geojobs.service.geojson;

import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

// Most ChatGPT-generated code
@Component
public class GeometryConverter {
  private static final double APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE = 111320.0;
  private final GeometryFactory geometryFactory = new GeometryFactory();

  public MultiPolygon apply(Point point, Double sizeInMeters) {
    var latitude = point.getX();
    var longitude = point.getY();

    // 1. Convert meters to degrees
    double halfSize = sizeInMeters / 2.0;
    double deltaLat = halfSize / APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE;
    double deltaLon =
        halfSize / (APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE * Math.cos(Math.toRadians(latitude)));

    // 2. Define square corners
    Coordinate[] coordinates =
        new Coordinate[] {
          new Coordinate(longitude - deltaLon, latitude - deltaLat),
          new Coordinate(longitude + deltaLon, latitude - deltaLat),
          new Coordinate(longitude + deltaLon, latitude + deltaLat),
          new Coordinate(longitude - deltaLon, latitude + deltaLat),
          new Coordinate(longitude - deltaLon, latitude - deltaLat) // Close ring
        };

    // 3. Build polygon and wrap in MultiPolygon
    LinearRing shell = geometryFactory.createLinearRing(coordinates);
    Polygon polygon = geometryFactory.createPolygon(shell, null);
    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
  }

  public MultiPolygon apply(List<List<List<List<BigDecimal>>>> multiPolygonData) {
    // multiPolygonData = List de Polygones
    Polygon[] polygons = new Polygon[multiPolygonData.size()];

    for (int i = 0; i < multiPolygonData.size(); i++) {
      List<List<List<BigDecimal>>> polygonData = multiPolygonData.get(i);
      if (polygonData.isEmpty()) {
        throw new IllegalArgumentException("Polygon must have at least one ring");
      }

      // Premier anneau = extérieur
      LinearRing shell = toLinearRing(polygonData.get(0));

      // Anneaux intérieurs = trous (optionnels)
      LinearRing[] holes = new LinearRing[Math.max(0, polygonData.size() - 1)];
      for (int j = 1; j < polygonData.size(); j++) {
        holes[j - 1] = toLinearRing(polygonData.get(j));
      }

      polygons[i] = geometryFactory.createPolygon(shell, holes);
    }

    return geometryFactory.createMultiPolygon(polygons);
  }

  public MultiPolygon unifyMultiPolygon(List<MultiPolygon> multiPolygons) {
    GeometryCollection collection =
        new GeometryCollection(multiPolygons.toArray(new Geometry[0]), geometryFactory);
    Geometry union = collection.union();
    // Cas 1 : déjà un MultiPolygon
    if (union instanceof MultiPolygon) {
      return (MultiPolygon) union;
    }

    // Cas 2 : un seul Polygon, on l'encapsule dans un MultiPolygon
    if (union instanceof Polygon) {
      return geometryFactory.createMultiPolygon(new Polygon[] {(Polygon) union});
    }
    throw new NotImplementedException("GeometryCollection not supported for now");
  }

  public List<List<List<List<BigDecimal>>>> multiPolygonToNestedList(MultiPolygon multiPolygon) {
    List<List<List<List<BigDecimal>>>> coordinates = new ArrayList<>();

    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
      List<List<List<BigDecimal>>> polygonList = new ArrayList<>();

      // Partie extérieure (shell)
      polygonList.add(linearRingToList(polygon.getExteriorRing()));

      // Trous éventuels (holes)
      for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
        polygonList.add(linearRingToList(polygon.getInteriorRingN(j)));
      }

      coordinates.add(polygonList);
    }

    return coordinates;
  }

  private List<List<BigDecimal>> linearRingToList(LineString ring) {
    List<List<BigDecimal>> coordsList = new ArrayList<>();

    for (Coordinate coord : ring.getCoordinates()) {
      List<BigDecimal> point =
          List.of(BigDecimal.valueOf(coord.getX()), BigDecimal.valueOf(coord.getY()));
      coordsList.add(point);
    }

    return coordsList;
  }

  private LinearRing toLinearRing(List<List<BigDecimal>> ringData) {
    Coordinate[] coordinates = new Coordinate[ringData.size()];
    for (int i = 0; i < ringData.size(); i++) {
      List<BigDecimal> point = ringData.get(i);
      if (point.size() < 2) {
        throw new IllegalArgumentException("Each point must have at least 2 coordinates (x, y)");
      }
      coordinates[i] = new Coordinate(point.get(0).doubleValue(), point.get(1).doubleValue());
    }
    return geometryFactory.createLinearRing(coordinates);
  }

  @SneakyThrows
  public String writeMultiPolygonAsString(MultiPolygon multiPolygon) {
    GeometryJSON geometryJSON = new GeometryJSON(15);
    StringWriter writer = new StringWriter();
    geometryJSON.write(multiPolygon, writer);
    return writer.toString();
  }
}
