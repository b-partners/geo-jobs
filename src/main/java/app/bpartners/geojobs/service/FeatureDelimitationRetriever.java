package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeatureDelimitationRetriever {

  public FeatureWithDelimitation apply(
      List<FeatureWithDelimitation> featureWithDelimitationList, Feature feature) {
    return featureWithDelimitationList.stream()
        .filter(
            f ->
                f.getRestFeature() != null
                    && f.getRestFeature().getGeometry() != null
                    && f.getRestFeature().getGeometry().equals(feature.getGeometry()))
        .findFirst()
        .orElse(
            featureWithDelimitationList.size() == 1
                    && featureWithDelimitationList.getFirst().getRestDelimitations() != null
                    && featureWithDelimitationList.getFirst().getRestDelimitations().size() == 1
                ? featureWithDelimitationList.getFirst()
                : null);
  }
}
