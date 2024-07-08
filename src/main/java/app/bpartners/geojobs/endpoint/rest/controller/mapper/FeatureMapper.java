package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeatureMapper {

  public Parcel toDomain(
      String parcelId, Feature rest, URL geoServerUrl, GeoServerParameter GeoServerParameter) {
    Parcel extractedParcel =
        Parcel.builder()
            .id(parcelId)
            .parcelContent(
                ParcelContent.builder()
                    .id(rest.getId())
                    .feature(rest)
                    .geoServerUrl(geoServerUrl)
                    .geoServerParameter(GeoServerParameter)
                    .creationDatetime(now())
                    .build())
            .build();
    return extractedParcel;
  }

  public static Feature from(TilingTask domainTask) {
    return domainTask.getParcelContent().getFeature();
  }

  public Polygon toDomain(Feature feature) {
    if (feature.getGeometry() == null) {
      throw new IllegalArgumentException("Multipolygon coordinates should not be null");
    }

    List<List<List<List<BigDecimal>>>> multiPolygonCoordinates =
        feature.getGeometry().getCoordinates();
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

    LinearRing linearRing =
        geometryFactory.createLinearRing(polygonCoords.toArray(new Coordinate[0]));

    return geometryFactory.createPolygon(linearRing);
  }

  public Feature toRest(Polygon domain) {
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
    feature.setGeometry(multiPolygon);

    return feature;
  }
}
