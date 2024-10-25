package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.BPLomModel;
import app.bpartners.geojobs.endpoint.rest.model.BPToitureModel;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

@Component
public class DetectionUpdateValidator implements BiConsumer<Detection, CreateDetection> {
  @Override
  public void accept(Detection detection, CreateDetection createDetection) {
    StringBuilder messageBuilder = new StringBuilder();
    if (detection.getProvidedGeoJsonZone() != null
        && !detection.getProvidedGeoJsonZone().equals(createDetection.getGeoJsonZone())) {
      messageBuilder
          .append(
              "Detection.geoJsonZone can not be updated once it has values, otherwise actual value"
                  + " ")
          .append(detection.getProvidedGeoJsonZone())
          .append(" is not equals provided value ")
          .append(createDetection.getGeoJsonZone())
          .append(". ");
    }
    var detectableObjectModel = createDetection.getDetectableObjectModel();
    boolean bpLomModelIsToBeUpdated =
        detection.getBpLomModel() != null
            && (detectableObjectModel == null
                || !(detectableObjectModel.getActualInstance() instanceof BPLomModel)
                || !detectableObjectModel.getBPLomModel().equals(detection.getBpLomModel()));
    boolean bpToitureModelIsToBeUpdated =
        detection.getBpToitureModel() != null
            && (detectableObjectModel == null
                || !(detectableObjectModel.getActualInstance() instanceof BPToitureModel)
                || !detectableObjectModel
                    .getBPToitureModel()
                    .equals(detection.getBpToitureModel()));
    if (bpLomModelIsToBeUpdated || bpToitureModelIsToBeUpdated) {
      messageBuilder
          .append(
              "Detection.detectableObjectModel can not be updated once it has values, otherwise"
                  + " actual value ")
          .append(detection.getDetectableObjectModel())
          .append(" is not equals provided value ")
          .append(detectableObjectModel)
          .append(". ");
    }
    if (detection.getGeoServerProperties() != null
        && !detection.getGeoServerProperties().equals(createDetection.getGeoServerProperties())) {
      messageBuilder
          .append(
              "Detection.geoServerProperties can not be updated once it has values, otherwise"
                  + " actual value ")
          .append(detection.getGeoServerProperties())
          .append(" is not equals provided value ")
          .append(createDetection.getGeoServerProperties())
          .append(". ");
    }
    String messageException = messageBuilder.toString();
    if (!messageException.isEmpty()) {
      throw new IllegalArgumentException(messageException);
    }
  }
}
