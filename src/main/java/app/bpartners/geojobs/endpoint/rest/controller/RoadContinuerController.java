package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.service.BucketServiceRoadContinuer;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.util.Map;
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
  @PostMapping("/road-continuer")
  public Map<String, String> roadContinuer(
      @RequestBody String geojson, @RequestParam Integer zoom, @RequestParam Integer imageSize) {
    var tilingConf = roadContinuerService.getTilingConf(zoom, imageSize);
    var geoJSONFile = roadContinuerService.continueRoute(geojson, tilingConf);
    return bsrc.getContinuedRoutePresignedUrl(geoJSONFile, adminApiKey);
  }
}
