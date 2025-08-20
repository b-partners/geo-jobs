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

  @PostMapping("/geojson/{id}/continue")
  public String continueGeoJson(
      @PathVariable("id") String id, @RequestParam("file") byte[] fileToUpload) {
    var file = fileWriter.apply(fileToUpload, FileWriter.createTempDirectory());
    geoJsonValidator.accept(file);
    return geoJsonContinuerService.generatePresignedUrl(id, file);
  }
}
