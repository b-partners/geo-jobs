package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.service.RoadContinuerService;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
public class RoadContinuerController {

  private RoadContinuerService roadContinuerService;

  @SneakyThrows
  @PostMapping("/road-continuer")
  public Map<String, String> roadContinuer(
      @RequestParam("geojson-file") MultipartFile geoJson,
      @RequestParam Integer zoom,
      @RequestParam Integer imageSize) {
    return roadContinuerService.continueRoute(geoJson, zoom, imageSize);
  }
}
