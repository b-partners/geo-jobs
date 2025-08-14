package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonValidator;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
public class GeoJsonContinuerController {
  private final GeoJsonContinuerService geoJsonContinuerService;
  private final GeoJsonValidator geoJsonValidator;

  @PostMapping(value = "/continue")
  public String continueGeoJson(
      @RequestParam("file") MultipartFile file,
      @RequestParam("imageSize") Integer imgSize,
      @RequestParam("zoom") Integer zoom)
      throws IOException {
    geoJsonValidator.accept(file);
    return geoJsonContinuerService.generatePresignedUrl(file, imgSize, zoom);
  }
}
