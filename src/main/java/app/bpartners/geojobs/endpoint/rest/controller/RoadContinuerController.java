package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.service.RoadContinuerService;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class RoadContinuerController {

  private RoadContinuerService roadContinuerService;

  @SneakyThrows
  @PostMapping("/road-continuer")
  public Map<String, String> roadContinuer(
      @RequestBody String geojson, @RequestParam Integer zoom, @RequestParam Integer imageSize) {
    return roadContinuerService.continueRoute(geojson, zoom, imageSize);
  }
}
