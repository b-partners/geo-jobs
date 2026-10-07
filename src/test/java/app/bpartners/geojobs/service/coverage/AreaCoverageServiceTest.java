package app.bpartners.geojobs.service.coverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import app.bpartners.geojobs.service.geodata.GeodataMapLayerClient;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.geodata.model.MapLayerActual;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AreaCoverageServiceTest {
  GeodataMapLayerClient mapLayerClientMock = mock();
  AreaCoverageService subject = new AreaCoverageService(mapLayerClientMock, new CoverageConf(5));

  private static MapLayerActual actualWithPrecision(Integer precisionInCm) {
    return new MapLayerActual(
        "https://wms",
        new GeodataMapLayer("id", "layer", 2024, "GEOSERVER", "75", "HOUSES_0", precisionInCm));
  }

  @Test
  void check_2d_is_covered_and_hd_when_precision_is_5_cm() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(5)));

    var actual = subject.check2D(2.3, 48.5);

    assertEquals(
        new Coverage2D(true, true, new ImageryLayer("layer", 2024, "GEOSERVER", 5)), actual);
  }

  @Test
  void check_2d_is_covered_but_not_hd_when_precision_is_over_threshold() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(10)));

    var actual = subject.check2D(2.3, 48.5);

    assertEquals(true, actual.covered());
    assertEquals(false, actual.hd());
  }

  @Test
  void check_2d_is_not_hd_when_precision_is_unknown() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(null)));

    var actual = subject.check2D(2.3, 48.5);

    assertEquals(true, actual.covered());
    assertEquals(false, actual.hd());
  }

  @Test
  void check_2d_is_not_covered_when_no_layer_is_reachable() {
    when(mapLayerClientMock.getActualMapLayer(0.0, 0.0)).thenReturn(Optional.empty());

    assertEquals(Coverage2D.notCovered(), subject.check2D(0.0, 0.0));
  }

  @Test
  void check_2d_propagates_gateway_timeout_instead_of_reporting_not_covered() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenThrow(new GatewayTimeoutException("down"));

    assertThrows(GatewayTimeoutException.class, () -> subject.check2D(2.3, 48.5));
  }
}
