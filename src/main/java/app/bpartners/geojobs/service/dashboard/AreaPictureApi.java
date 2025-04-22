package app.bpartners.geojobs.service.dashboard;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import org.springframework.stereotype.Component;

@Component
public class AreaPictureApi {

  public AreaPictureDetails crupdateAreaPictureDetails(
      CrupdateAreaPictureDetails crupdateAreaPictureDetails) {
    throw new NotImplementedException("Not implemented");
  }
}
