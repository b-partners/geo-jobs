package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.service.BucketServiceRoadContinuer;
import app.bpartners.geojobs.service.RoadContinuerService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@NoArgsConstructor
@RestController
public class RoadContinuerController {

  private RoadContinuerService roadContinuerService;
  private BucketServiceRoadContinuer bsrc;

  @Value("${admin.api.key}")
  private String adminApiKey;

  @SneakyThrows
  @PostMapping("/roadcontinuer")
  public String roadContinuer(
      @RequestBody String geojson, @RequestParam int zoom, @RequestParam int imageSize) {
    var tilingConf = new TilingConf(zoom, imageSize);
    var gj = roadContinuerService.continueRoute(geojson, tilingConf);
    return bsrc.getContinuedRoutePresignedUrl(gj, adminApiKey);
  }
}
