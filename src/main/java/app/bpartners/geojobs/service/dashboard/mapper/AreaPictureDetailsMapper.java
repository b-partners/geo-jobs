package app.bpartners.geojobs.service.dashboard.mapper;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import org.springframework.stereotype.Component;

@Component
public class AreaPictureDetailsMapper {

  public CrupdateAreaPictureDetails toCrupdateAreaPictureDetails(String address) {
    throw new NotImplementedException("Not implemented yet");
  }

  public Feature toFeature(AreaPictureDetails areaPictureDetails) {
    throw new NotImplementedException("Not implemented yet");
  }
}
