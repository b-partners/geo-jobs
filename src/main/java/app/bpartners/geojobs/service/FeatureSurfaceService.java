package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.parcelization.area.AreaComputer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureSurfaceService {
  private static final AreaComputer areaComputer = new AreaComputer();
  private final FeatureMapper featureMapper;

  double getSurface(Feature feature) {
    var areaValue = areaComputer.apply(featureMapper.toDomain(feature));
    return areaValue.getValue();
  }

  double getSurface(List<Feature> features) {
    return features.stream().mapToDouble(this::getSurface).sum();
  }
}
