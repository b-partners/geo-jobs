package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum.HOUSES_0;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.GeoPosition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// TODO : add unit mock test
@Disabled("TODO: local use only, disable otherwise")
class AreaPictureApiIT {
  ApiConfiguration apiConfiguration = new ApiConfiguration(System.getenv("BPARTNERS_API_URL"));
  final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  SecurityApi securityApi = new SecurityApi(apiConfiguration, objectMapper);
  UserAccountsApi userAccountsApi = new UserAccountsApi(apiConfiguration, securityApi);
  AreaPictureApi subject = new AreaPictureApi(apiConfiguration, userAccountsApi);
  final String apiKey = System.getenv("API_KEY");

  @Test
  void create_area_picture() {
    var areaPictureId = randomUUID().toString();
    var fileId = randomUUID().toString();
    var address = "1 Rue Benjamin Franklin, 75016 Paris, France";
    var crupdateAreaPictureDetails =
        new CrupdateAreaPictureDetails(
            address,
            0,
            fileId,
            address + "-" + hashCode(),
            "944aa3af-4360-4686-a606-2d580c56449a",
            HOUSES_0);

    var actual =
        subject.crupdateAreaPictureDetails(areaPictureId, crupdateAreaPictureDetails, apiKey);

    var expected = expectedAreaPictureDetails(actual);
    assertEquals(expected, actual);
  }

  private static @NotNull AreaPictureDetails expectedAreaPictureDetails(AreaPictureDetails actual) {
    return new AreaPictureDetails(
        actual.id(),
        new AreaPictureMapLayer(
            actual.actualLayer().id(),
            "cite:PHOTO_AERIENNE",
            new AreaPictureMapLayer.Zoom("HOUSES_0", 20)),
        new GeoPosition(48.8589892, 2.2847458));
  }
}
