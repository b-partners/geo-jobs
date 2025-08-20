package app.bpartners.geojobs.service.dashboard.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.GeoPosition;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.Building;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.BuildingAddress;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.BuildingStatus;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry.Geometry;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry.MockGeometryCoordinates;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AreaPictureDetailsMapperTest {
  BuildingApi mockBuildingApi;
  GeometryConverter geometryConverter;
  AreaPictureDetailsMapper subject;

  AreaPictureDetails areaPictureDetails;

  @BeforeEach
  void setup() {
    mockBuildingApi = Mockito.mock(BuildingApi.class);
    geometryConverter = new GeometryConverter(mockBuildingApi);
    subject = new AreaPictureDetailsMapper(geometryConverter);

    areaPictureDetails =
        new AreaPictureDetails(
            "id",
            new AreaPictureMapLayer(
                "layer_id", "buildings_layer", new AreaPictureMapLayer.Zoom("BUILDINGS", 16)),
            new GeoPosition(48.8777709, 2.3300819));

    when(mockBuildingApi.getNearestBuildingAt(2.3300819, 48.8777709, 100))
        .thenReturn(
            new Building(
                "37DAG17MWRCD",
                BuildingStatus.constructed,
                new Geometry(
                    app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT,
                    MockGeometryCoordinates.getPointCoordinates()),
                new Geometry(
                    app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON,
                    MockGeometryCoordinates.getMultiPolygonCoordinates()),
                List.of(
                    new BuildingAddress(
                        "75109_5741_00008",
                        "bdnb",
                        "8",
                        "",
                        "rues de londres",
                        "Paris 9e Arrondissement",
                        "75009",
                        75056)),
                0d));
  }

  @Test
  void to_crupdate_area_picture_details() {
    var actual = subject.toCrupdateAreaPictureDetails("random address");

    assertEquals("random address", actual.address());
  }

  @Test
  void to_feature() {
    var actual = subject.toFeature(areaPictureDetails, "address");

    assertEquals(areaPictureDetails.actualLayer().maximumZoom().number(), actual.getZoom());
    assertEquals(4, actual.getProperties().size());
    assertEquals(
        areaPictureDetails.actualLayer().name(), actual.getProperties().get("priorityLayer"));
  }
}
