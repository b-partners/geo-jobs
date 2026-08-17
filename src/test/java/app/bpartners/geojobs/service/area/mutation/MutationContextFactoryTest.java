package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DetectionMaskFromTileRetriever;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class MutationContextFactoryTest {
  private final DetectionMaskFromTileRetriever maskFromTileRetrieverMock = mock();
  private final MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  private final GeometryConverter geometryConverterMock = mock();
  private final MutationContextFactory subject =
      new MutationContextFactory(
          maskFromTileRetrieverMock, machineDetectedTileRepositoryMock, geometryConverterMock);

  @Test
  void create_returns_null_until_parcel_grouping_by_date_is_implemented() {
    var detectionMock = mock(Detection.class);
    var roofGeometryMock = mock(Geometry.class);

    var actual = subject.create(detectionMock, roofGeometryMock);

    assertNull(actual);
    verifyNoInteractions(
        detectionMock,
        roofGeometryMock,
        maskFromTileRetrieverMock,
        machineDetectedTileRepositoryMock,
        geometryConverterMock);
  }
}
