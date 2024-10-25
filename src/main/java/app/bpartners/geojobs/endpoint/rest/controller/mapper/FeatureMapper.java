package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeatureMapper {
  public Parcel toDomain(
      String parcelId, Feature rest, URL geoServerUrl, GeoServerParameter GeoServerParameter) {
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(
            ParcelContent.builder()
                .id(rest.getId())
                .feature(toDomainFeature(rest))
                .geoServerUrl(geoServerUrl)
                .geoServerParameter(GeoServerParameter)
                .creationDatetime(now())
                .build())
        .build();
  }

  public static Feature from(TilingTask domainTask) {
    return domainTask.getParcelContent().getFeature();
  }

  public static app.bpartners.geojobs.repository.model.Feature toDomainFeature(Feature rest) {
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .id(rest.getId())
        .zoom(rest.getZoom())
        .geometry(toDomainFeatureGeometry(rest.getGeometry()))
        .build();
  }

  public static Feature toRestFeature(app.bpartners.geojobs.repository.model.Feature domain) {
    if (domain == null || domain.getGeometry() == null) {
      return null;
    }
    return new Feature()
        .id(domain.getId())
        .zoom(domain.getZoom())
        .geometry(toRestFeatureGeometry(domain.getGeometry()));
  }

  @SneakyThrows
  private static app.bpartners.geojobs.repository.model.Feature.FeatureGeometry
      toDomainFeatureGeometry(FeatureGeometry featureGeometry) {
    var actualInstance = featureGeometry.getActualInstance();
    return app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
        .geometryType(getGeometryType(actualInstance))
        .actualInstanceStringValue(
            objectMapper().writeValueAsString(featureGeometry.getActualInstance()))
        .build();
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
      default -> throw new IllegalArgumentException("Unknown geometry " + type);
    };
  }

  public org.locationtech.jts.geom.Polygon toDomain(Feature feature) {
    List<List<List<List<BigDecimal>>>> multiPolygonCoordinates = validateFeature(feature);
    GeometryFactory geometryFactory = new GeometryFactory();
    List<Coordinate> polygonCoords = new ArrayList<>();

    multiPolygonCoordinates
        .getFirst()
        .forEach(
            ring -> {
              Coordinate[] ringCoords =
                  ring.stream()
                      .map(
                          point ->
                              new Coordinate(
                                  point.getFirst().doubleValue(), point.getLast().doubleValue()))
                      .toArray(Coordinate[]::new);
              polygonCoords.addAll(List.of(ringCoords));
            });

    log.info("To be linear {}", Arrays.deepToString(polygonCoords.toArray(new Coordinate[0])));
    LinearRing linearRing =
        geometryFactory.createLinearRing(polygonCoords.toArray(new Coordinate[0]));

    return geometryFactory.createPolygon(linearRing);
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
    if (clazz.equals(Polygon.class)) {
      return List.of(Objects.requireNonNull(geometry.getPolygon().getCoordinates()));
    }
    if (clazz.equals(Point.class)) {
      // Repeat the same point on itself to stay within the area
      var point = List.of(List.of(Objects.requireNonNull(geometry.getPoint().getCoordinates())));
      return List.of(point, point, point, point);
    }
    throw new NotImplementedException(
        "Only MultiPolygon, Polygon, Point geometry is supported for now when mapping feature to"
            + " Polygon, but actual geometry class is : "
            + geometry.getActualInstance().getClass());
  }

  public Feature toRest(org.locationtech.jts.geom.Polygon domain, String id) {
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
    feature.setId(id);
    multiPolygon.setType(MULTI_POLYGON);
    feature.setGeometry(new FeatureGeometry(multiPolygon));

    return feature;
  }
}
