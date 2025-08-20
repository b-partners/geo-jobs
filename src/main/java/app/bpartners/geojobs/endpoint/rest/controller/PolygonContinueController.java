package app.bpartners.geojobs.endpoint.rest.controller;

import java.util.Map;
import app.bpartners.geojobs.endpoint.rest.validator.FileGeoJsonValidator;
import app.bpartners.geojobs.service.PolygonContinue.PolygonContinueService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
public class PolygonContinueController {
    private final PolygonContinueService polygonContinueService;
    private final FileGeoJsonValidator fileGeoJsonValidator;

    @PostMapping("/polygon-continue")
    public Map<String, String> continuePolygons(
            @RequestParam("file") MultipartFile file
    ) {
        fileGeoJsonValidator.accept(file);
        return polygonContinueService.PolygonsContinueAsync(file);
    }
}