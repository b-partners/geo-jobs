package app.bpartners.geojobs.service.dashboard.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum.HOUSES_0;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AreaPictureDetailsMapper {
  private static final int DEFAULT_SHIFT_NB = 0;
  private static final int DEFAULT_POLYGON_SIZE_IN_METERS = 100;
  private static final String FEATURE_ADDRESS_PROPERTY = "address";
  private final GeometryConverter geometryConverter;
  private final BuildingApi buildingApi;

  public CrupdateAreaPictureDetails toCrupdateAreaPictureDetails(String address) {
    var fileId = randomUUID().toString();
    var filename = address + "-" + randomUUID();
    return new CrupdateAreaPictureDetails(
        address, DEFAULT_SHIFT_NB, null, fileId, filename, null, HOUSES_0);
  }

  public Feature toFeature(AreaPictureDetails areaPictureDetails, String address) {
    var featureId = randomUUID().toString();
    var layer = areaPictureDetails.actualLayer();
    int zoom = layer.maximumZoom().number();
    var multiPolygon = toMultiPolygon(areaPictureDetails);
    var properties = new HashMap<String, Object>();
    properties.put(FEATURE_ADDRESS_PROPERTY, address);
    properties.put("id", featureId);
    properties.put("zoom", zoom);
    properties.put("priorityLayer", layer.name());
    return Feature.builder()
        .id(featureId)
        .zoom(zoom)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    geometryConverter.writeMultiPolygonAsString(multiPolygon))
                .build())
        .properties(properties)
        .build();
  }

  private MultiPolygon toMultiPolygon(AreaPictureDetails areaPictureDetails) {
    var point =
        new Point()
            .x(areaPictureDetails.currentGeoPosition().latitude())
            .y(areaPictureDetails.currentGeoPosition().longitude());
    var nearestBuilding =
        buildingApi.getNearestBuildingAt(
            point.getX(), point.getY(), DEFAULT_POLYGON_SIZE_IN_METERS);
    var multiPolygonCoordinates = nearestBuilding.shape().getMultiPolygonCoordinates();
    return geometryConverter.apply(multiPolygonCoordinates);
  }
}
