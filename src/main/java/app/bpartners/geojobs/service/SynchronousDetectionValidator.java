package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SynchronousDetectionValidator implements Consumer<CreateDetection> {
  private final GeometryTiledValidator geometryTiledValidator;

  @Override
  public void accept(CreateDetection createDetection) {
    var modelName = createDetection.getDetectableObjectModel().getModelName();
    var geoJsonZone = createDetection.getGeoJsonZone();
    if (!TOITURE.equals(modelName)) {
      throw new NotImplementedException(
          "Only BP_TOITURE detection model is supported for now,"
              + " otherwise, model provided is "
              + modelName);
    }
    if (geoJsonZone.size() != 1) {
      throw new NotImplementedException(
          "Only one feature supported, otherwise provided geoJson features.size = "
              + geoJsonZone.size());
    }
    var providedGeoJsonIsContainedInFrame =
        geometryTiledValidator.apply(geoJsonZone.getFirst().getGeometry().getActualInstance());
    if (!providedGeoJsonIsContainedInFrame) {
      throw new NotImplementedException(
          "Provided geojson polygon is too large to be processed synchronously");
    }
  }
}
