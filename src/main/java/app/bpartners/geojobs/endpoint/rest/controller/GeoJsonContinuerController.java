package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonValidator;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class GeoJsonContinuerController {
  private final GeoJsonContinuerService geoJsonContinuerService;
  private final GeoJsonValidator geoJsonValidator;
  private final FileWriter fileWriter;

  @PostMapping(value = "/continue")
  public String continueGeoJson(
      @RequestParam("file") byte[] fileToUpload,
      @RequestParam("imageSize") Integer imgSize,
      @RequestParam("zoom") Integer zoom) {
    var file = fileWriter.apply(fileToUpload, null);
    geoJsonValidator.accept(file);
    return geoJsonContinuerService.generatePresignedUrl(file, imgSize, zoom);
  }
}
