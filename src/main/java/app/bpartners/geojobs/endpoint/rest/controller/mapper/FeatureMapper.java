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
    var multiPolygonCoordinates = feature.getGeometry().getCoordinates();
    GeometryFactory geometryFactory = new GeometryFactory();
    List<Coordinate> polygonCoords = new ArrayList<>();

    if (multiPolygonCoordinates == null || multiPolygonCoordinates.isEmpty()) {
      throw new IllegalArgumentException("Multipolygon coordinates should not be null");
    }

    List<List<List<BigDecimal>>> polygon = multiPolygonCoordinates.getFirst();
    for (List<List<BigDecimal>> ring : polygon) {
      Coordinate[] ringCoords = new Coordinate[ring.size()];
      for (int i = 0; i < ring.size(); i++) {
        BigDecimal x = ring.get(i).getFirst();
        BigDecimal y = ring.get(i).getLast();
        ringCoords[i] = new Coordinate(x.doubleValue(), y.doubleValue());
      }

      polygonCoords.addAll(List.of(ringCoords));
    }

    LinearRing linearRing =
        geometryFactory.createLinearRing(polygonCoords.toArray(new Coordinate[0]));

    return geometryFactory.createPolygon(linearRing);
  }

  public Feature toRest(Polygon domain) {
    List<List<List<List<BigDecimal>>>> multipolygonCoordinates = new ArrayList<>();
    Coordinate[] polygonCoordinates = domain.getCoordinates();
    List<List<List<BigDecimal>>> polygonCoords = new ArrayList<>();
    List<List<BigDecimal>> ringCoords = new ArrayList<>();

    for (Coordinate coord : polygonCoordinates) {
      List<BigDecimal> point = new ArrayList<>();
      point.add(BigDecimal.valueOf(coord.getX()));
      point.add(BigDecimal.valueOf(coord.getY()));
      ringCoords.add(point);
    }

    polygonCoords.add(ringCoords);
    multipolygonCoordinates.add(polygonCoords);

    MultiPolygon multiPolygon = new MultiPolygon().coordinates(multipolygonCoordinates);
    Feature feature = new Feature();
    feature.setGeometry(multiPolygon);

    return feature;
  }
}
