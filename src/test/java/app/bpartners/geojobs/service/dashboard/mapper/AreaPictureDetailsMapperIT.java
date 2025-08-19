package app.bpartners.geojobs.service.dashboard.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.GeoPosition;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import org.junit.jupiter.api.Test;

class AreaPictureDetailsMapperIT {

  BuildingApi buildingApi = new BuildingApi();
  GeometryConverter geometryConverter = new GeometryConverter(buildingApi);
  AreaPictureDetailsMapper subject = new AreaPictureDetailsMapper(geometryConverter);

  AreaPictureDetails areaPictureDetails =
      new AreaPictureDetails(
          "id",
          new AreaPictureMapLayer(
              "layer_id", "test_layer", new AreaPictureMapLayer.Zoom("BUILDINGS", 4)),
          new GeoPosition(48.852500d, 2.319917d));

  @Test
  void to_feature() {
    var actual = subject.toFeature(areaPictureDetails, "address");

    assertEquals(4, actual.getZoom());
    assertEquals(4, actual.getProperties().size());
    assertEquals("test_layer", actual.getProperties().get("priorityLayer"));
  }

  @Test
  void to_crupdate_area_picture_details() {
    var actual = subject.toCrupdateAreaPictureDetails("address");

    assertEquals("address", actual.address());
  }
}
