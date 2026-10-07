package app.bpartners.geojobs.service.coverage;

import app.bpartners.geojobs.service.geodata.GeodataMapLayerClient;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AreaCoverageService {
  private final GeodataMapLayerClient mapLayerClient;
  private final CoverageConf conf;

  public Coverage2D check2D(double longitude, double latitude) {
    return mapLayerClient
        .getActualMapLayer(latitude, longitude)
        .map(actual -> toCoverage2D(actual.layer()))
        .orElseGet(Coverage2D::notCovered);
  }

  private Coverage2D toCoverage2D(GeodataMapLayer layer) {
    if (layer == null) {
      return Coverage2D.notCovered();
    }
    var precision = layer.precisionLevelInCm();
    var hd = precision != null && precision <= conf.getHdMaxPrecisionInCm();
    return new Coverage2D(
        true, hd, new ImageryLayer(layer.name(), layer.year(), layer.source(), precision));
  }
}
