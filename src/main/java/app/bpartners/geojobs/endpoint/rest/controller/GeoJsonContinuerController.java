package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
public class GeoJsonContinuerController {
  private final GeoJsonContinuerService geoJsonContinuerService;

  @PostMapping(value = "/continue")
  public String continueGeoJson(@RequestParam("file") MultipartFile file) throws IOException {
    return geoJsonContinuerService.generatePresignedUrl(file);
  }
}
