package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.dashboard.component.FileType.AREA_PICTURE;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.FileApi;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AreaPictureDetailsController {
  private final AreaPictureApi areaPictureApi;
  private final FileApi fileApi;
  private final String adminApiKey;

  public AreaPictureDetailsController(
      AreaPictureApi areaPictureApi,
      FileApi fileApi,
      @Value("${admin.api.key}") String adminApiKey) {
    this.areaPictureApi = areaPictureApi;
    this.fileApi = fileApi;
    this.adminApiKey = adminApiKey;
  }

  @GetMapping("/areaPictureDetails")
  public AreaPictureDetails getAreaPictureDetails(@RequestParam String address) {
    var areaPictureId = randomUUID().toString();
    var fileId = randomUUID().toString();
    try {
      areaPictureApi.crupdateAreaPictureDetails(
          areaPictureId,
          new CrupdateAreaPictureDetails(
              address, 0, fileId, address + hashCode(), null, ZoneTilingJob.ZoomLevelEnum.HOUSES_0),
          adminApiKey);
      byte[] imageAsBytes = fileApi.downloadOrUploadFile(fileId, AREA_PICTURE, adminApiKey);
      return new AreaPictureDetails()
          .address(address)
          .imageBase64(Base64.getEncoder().encodeToString(imageAsBytes));
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      throw new ApiException(
          SERVER_EXCEPTION, "Unable to retrieve area picture details from address : " + address);
    }
  }
}
