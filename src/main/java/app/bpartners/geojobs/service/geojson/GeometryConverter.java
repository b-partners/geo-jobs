package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;

// Most ChatGPT-generated code
@Component
public class GeometryConverter {
  private static final int DEFAULT_POLYGON_SIZE_IN_METERS = 100;
  private static final double APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE = 111320.0;
  private final GeometryFactory geometryFactory = new GeometryFactory();
  private final BuildingApi buildingApi;

  public GeometryConverter(BuildingApi buildingApi) {
    this.buildingApi = buildingApi;
  }

  @SneakyThrows
  public Feature toFeature(
      Integer zoom,
      HashMap<String, Object> properties,
      app.bpartners.geojobs.endpoint.rest.model.Point restPoint) {
    return Feature.builder()
        .zoom(zoom)
        .properties(properties)
        .geometry(
            new Feature.FeatureGeometry(
                app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT,
                new ObjectMapper().writeValueAsString(restPoint)))
        .build();
  }

  public Feature toFeature(
      String featureId,
      Integer zoom,
      HashMap<String, Object> properties,
      MultiPolygon multiPolygon) {
    return Feature.builder()
        .id(featureId)
        .zoom(zoom)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(writeMultiPolygonAsString(multiPolygon))
                .build())
        .properties(properties)
        .build();
  }

  public MultiPolygon retrieveNearestRoofMultiPolygon(
      app.bpartners.geojobs.endpoint.rest.model.Point point) {
    if (buildingApi == null || point == null) {
      return null;
    }
    return retrieveNearestRoofMultiPolygon(point.getCoordinates());
  }

  public MultiPolygon retrieveNearestRoofMultiPolygon(List<BigDecimal> coordinates) {
    var longitude = coordinates.getFirst();
    var latitude = coordinates.getLast();
    var nearestBuilding =
        buildingApi.getNearestBuildingAt(
            longitude.doubleValue(), latitude.doubleValue(), DEFAULT_POLYGON_SIZE_IN_METERS);
    var multiPolygonCoordinates = nearestBuilding.shape().getMultiPolygonCoordinates();
    return apply(multiPolygonCoordinates);
  }

  @SneakyThrows
  public List<BigDecimal> centroidFromGeometry(Object featureInstance) {
    ObjectMapper objectMapper = new ObjectMapper();
    Geometry geometry;
    switch (featureInstance) {
      case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon multiPolygon ->
          geometry = readGeometryFromString(objectMapper.writeValueAsString(multiPolygon));
      case app.bpartners.geojobs.endpoint.rest.model.Polygon polygon ->
          geometry = readGeometryFromString(objectMapper.writeValueAsString(polygon));
      case app.bpartners.geojobs.endpoint.rest.model.Point point ->
          geometry = readGeometryFromString(objectMapper.writeValueAsString(point));
      default ->
          throw new UnsupportedOperationException(
              "Unsupported feature instance: " + featureInstance);
    }
    Coordinate centroid = geometry.getCentroid().getCoordinate();
    return List.of(BigDecimal.valueOf(centroid.x), BigDecimal.valueOf(centroid.y));
  }

  public MultiPolygon apply(
      app.bpartners.geojobs.endpoint.rest.model.Point point, Double sizeInMeters) {
    var longitude = point.getCoordinates().getFirst().doubleValue();
    var latitude = point.getCoordinates().getLast().doubleValue();

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

  public org.locationtech.jts.geom.Polygon toPolygon(
      List<List<List<List<BigDecimal>>>> multiPolygonCoordinates) {
    GeometryFactory geometryFactory = new GeometryFactory();

    List<List<BigDecimal>> firstRing = multiPolygonCoordinates.getFirst().getFirst();
    Coordinate[] ringCoords =
        firstRing.stream()
            .map(
                point ->
                    new Coordinate(point.getFirst().doubleValue(), point.getLast().doubleValue()))
            .toArray(Coordinate[]::new);

    if (!ringCoords[0].equals2D(ringCoords[ringCoords.length - 1])) {
      Coordinate[] closedRingCoords = Arrays.copyOf(ringCoords, ringCoords.length + 1);
      closedRingCoords[closedRingCoords.length - 1] = closedRingCoords[0];
      ringCoords = closedRingCoords;
    }

    LinearRing shell = geometryFactory.createLinearRing(ringCoords);
    return geometryFactory.createPolygon(shell);
  }

  public List<List<BigDecimal>> polygonToPoints(Polygon polygon) {
    List<List<BigDecimal>> result = new ArrayList<>();

    Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();

    for (Coordinate coord : coordinates) {
      List<BigDecimal> point = new ArrayList<>(2);
      point.add(BigDecimal.valueOf(coord.getX()));
      point.add(BigDecimal.valueOf(coord.getY()));
      result.add(point);
    }

    return result;
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

  @SneakyThrows
  public String writeGeometryAsString(Geometry geometry) {
    GeometryJSON geometryJSON = new GeometryJSON(15);
    StringWriter writer = new StringWriter();
    geometryJSON.write(geometry, writer);
    return writer.toString();
  }

  @SneakyThrows
  public Geometry readGeometryFromString(String geoJsonString) {
    GeometryJSON geometryJSON = new GeometryJSON(15);
    return geometryJSON.read(new StringReader(geoJsonString));
  }

  private double[] tileXYToLonLatBounds(int x, int y, int z) {
    double n = Math.pow(2, z);

    double lonLeft = x / n * 360.0 - 180.0;
    double lonRight = (x + 1) / n * 360.0 - 180.0;

    double latTop = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n))));
    double latBottom = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (y + 1) / n))));

    return new double[] {lonLeft, latBottom, lonRight, latTop}; // [west, south, east, north]
  }

  public MultiPolygon getMultiPolygonFromTile(int x, int y, int zoom) {
    double[] bbox = tileXYToLonLatBounds(x, y, zoom);
    double west = bbox[0];
    double south = bbox[1];
    double east = bbox[2];
    double north = bbox[3];

    GeometryFactory gf = new GeometryFactory();
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(west, south),
          new Coordinate(west, north),
          new Coordinate(east, north),
          new Coordinate(east, south),
          new Coordinate(west, south) // fermeture du polygone
        };

    LinearRing shell = gf.createLinearRing(coords);
    Polygon polygon = gf.createPolygon(shell, null);
    return gf.createMultiPolygon(new Polygon[] {polygon});
  }
}
