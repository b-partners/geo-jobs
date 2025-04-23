package app.bpartners.geojobs.service.dashboard.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum.HOUSES_0;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.service.geojson.PointToMultiPolygonConverter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AreaPictureDetailsMapper {
  private static final int DEFAULT_SHIFT_NB = 0;
  private static final double DEFAULT_POLYGON_SIZE_IN_METERS = 70.0;
  private final PointToMultiPolygonConverter pointToMultiPolygonConverter;

  public CrupdateAreaPictureDetails toCrupdateAreaPictureDetails(String address) {
    var fileId = randomUUID().toString();
    var filename = address + "-" + hashCode();
    return new CrupdateAreaPictureDetails(
        address, DEFAULT_SHIFT_NB, fileId, filename, null, HOUSES_0);
  }

  public Feature toFeature(AreaPictureDetails areaPictureDetails) {
    var featureId = randomUUID().toString();
    var layer = areaPictureDetails.actualLayer();
    int zoom = layer.maximumZoom().number();
    var multiPolygon = toMultiPolygon(areaPictureDetails);
    return Feature.builder()
        .id(featureId)
        .zoom(zoom)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    pointToMultiPolygonConverter.generateSquareMultiPolygon(multiPolygon))
                .build())
        .build();
  }

  private MultiPolygon toMultiPolygon(AreaPictureDetails areaPictureDetails) {
    var point =
        new Point()
            .x(areaPictureDetails.currentGeoPosition().latitude())
            .y(areaPictureDetails.currentGeoPosition().longitude());
    return pointToMultiPolygonConverter.apply(point, DEFAULT_POLYGON_SIZE_IN_METERS);
  }
}
