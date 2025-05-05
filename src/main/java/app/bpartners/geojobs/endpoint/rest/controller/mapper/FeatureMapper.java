package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.springframework.stereotype.Component;

@Component
public class FeatureMapper {

  public Parcel toDomain(
      String parcelId, Feature rest, URL geoServerUrl, GeoServerParameter GeoServerParameter) {
    var id =
        Objects.requireNonNull(rest.getProperties()).get("id") == null
            ? null
            : rest.getProperties().get("id").toString();
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(
            ParcelContent.builder()
                .id(id)
                .feature(toDomainFeature(rest))
                .geoServerUrl(geoServerUrl)
                .geoServerParameter(GeoServerParameter)
                .creationDatetime(now())
                .build())
        .build();
  }

  public static Feature from(ParcelTilingTask domainTask) {
    return domainTask.getParcelContent() == null
        ? null
        : domainTask.getParcelContent().restFeatures();
  }

  public static app.bpartners.geojobs.repository.model.Feature toDomainFeature(Feature rest) {
    HashMap<String, Object> properties =
        rest.getProperties() == null ? new HashMap<>() : new HashMap<>(rest.getProperties());
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .id(properties.get("id") == null ? null : properties.get("id").toString())
        .zoom(properties.get("zoom") == null ? null : (Integer) properties.get("zoom"))
        .geometry(toDomainFeatureGeometry(rest.getGeometry()))
        .properties(properties)
        .build();
  }

  public static Feature toRestFeature(app.bpartners.geojobs.repository.model.Feature domain) {
    if (domain == null || domain.getGeometry() == null) {
      return null;
    }
    var restFeatureGeometry = toRestFeatureGeometry(domain.getGeometry());
    return new Feature()
        .type(FEATURE)
        .geometry(restFeatureGeometry)
        .properties(domain.getProperties());
  }

  @SneakyThrows
  private static app.bpartners.geojobs.repository.model.Feature.FeatureGeometry
      toDomainFeatureGeometry(FeatureGeometry featureGeometry) {
    var actualInstance = featureGeometry.getActualInstance();
    var featureDomain =
        app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
            .geometryType(getGeometryType(actualInstance))
            .actualInstanceStringValue(
                objectMapper().writeValueAsString(featureGeometry.getActualInstance()))
            .build();
    return featureDomain;
  }

  private static Geometry.TypeEnum getGeometryType(Object actualInstance) {
    var clazz = actualInstance.getClass();
    if (clazz.equals(MultiPolygon.class)) {
      return Geometry.TypeEnum.MULTI_POLYGON;
    }
    if (clazz.equals(Polygon.class)) {
      return Geometry.TypeEnum.POLYGON;
    }
    if (clazz.equals(Point.class)) {
      return Geometry.TypeEnum.POINT;
    }
    throw new IllegalArgumentException("Unknown geometry" + clazz);
  }

  @SneakyThrows
  private static FeatureGeometry toRestFeatureGeometry(
      app.bpartners.geojobs.repository.model.Feature.FeatureGeometry featureGeometry) {
    var actualInstanceStringValue = featureGeometry.getActualInstanceStringValue();
    var type = featureGeometry.getGeometryType();
    if (actualInstanceStringValue == null || type == null) {
      return null;
    }
    return switch (type) {
      case POINT ->
          new FeatureGeometry(objectMapper().readValue(actualInstanceStringValue, Point.class));
      case POLYGON ->
          new FeatureGeometry(
              objectMapper()
                  .readValue(
                      actualInstanceStringValue,
                      app.bpartners.geojobs.endpoint.rest.model.Polygon.class));
      case MULTI_POLYGON ->
          new FeatureGeometry(
              objectMapper().readValue(actualInstanceStringValue, MultiPolygon.class));
    };
  }

  public org.locationtech.jts.geom.Polygon toDomain(Feature feature) {
    List<List<List<List<BigDecimal>>>> multiPolygonCoordinates = validateFeature(feature);
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

  public List<org.locationtech.jts.geom.Polygon> toDomainList(Feature feature) {
    List<List<List<List<BigDecimal>>>> multiPolygonCoordinates = validateFeature(feature);
    GeometryFactory geometryFactory = new GeometryFactory();
    List<List<Coordinate>> polygonCoords = new ArrayList<>();

    multiPolygonCoordinates.forEach(
        ring -> {
          List<Coordinate> coords = new ArrayList<>();
          ring.forEach(
              geo -> {
                Coordinate[] ringCoords =
                    geo.stream()
                        .map(
                            point ->
                                new Coordinate(
                                    point.getFirst().doubleValue(), point.getLast().doubleValue()))
                        .toArray(Coordinate[]::new);
                coords.addAll(List.of(ringCoords));
              });
          polygonCoords.add(coords);
        });

    return polygonCoords.stream()
        .map(
            coordinates -> {
              LinearRing linearRing =
                  geometryFactory.createLinearRing(coordinates.toArray(new Coordinate[0]));
              return geometryFactory.createPolygon(linearRing);
            })
        .toList();
  }

  @Nullable
  private List<List<List<List<BigDecimal>>>> validateFeature(Feature feature) {
    if (feature.getGeometry() == null) {
      throw new IllegalArgumentException("Geometry must not be null");
    }
    FeatureGeometry geometry = feature.getGeometry();
    var clazz = geometry.getActualInstance().getClass();
    if (clazz.equals(MultiPolygon.class)) {
      return geometry.getMultiPolygon().getCoordinates();
    }
    throw new NotImplementedException(
        "Only MultiPolygon geometry is supported for now when mapping feature to"
            + " Polygon, but actual geometry class is : "
            + geometry.getActualInstance().getClass());
  }

  public Feature toRest(org.locationtech.jts.geom.Polygon domain, int zoom, String id) {
    List<List<List<List<BigDecimal>>>> multiPolygonCoordinates = new ArrayList<>();
    Coordinate[] polygonCoordinates = domain.getCoordinates();

    List<List<BigDecimal>> ringCoords =
        Arrays.stream(polygonCoordinates)
            .map(
                coord ->
                    List.of(BigDecimal.valueOf(coord.getX()), BigDecimal.valueOf(coord.getY())))
            .toList();

    List<List<List<BigDecimal>>> polygonCoords = new ArrayList<>();
    polygonCoords.add(ringCoords);
    multiPolygonCoordinates.add(polygonCoords);

    MultiPolygon multiPolygon = new MultiPolygon().coordinates(multiPolygonCoordinates);
    Feature feature = new Feature();
    feature.getProperties().put("id", id);
    feature.getProperties().put("zoom", zoom);
    multiPolygon.setType(MULTI_POLYGON);
    feature.setGeometry(new FeatureGeometry(multiPolygon));

    return feature;
  }
}
