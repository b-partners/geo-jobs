package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.mapper.FileMapper;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;


@AllArgsConstructor
@RestController
public class GeoJsonContinuerController {
    private final GeoJsonContinuerService geoJsonContinuerService;
    private final FileMapper fileMapper;

    @GetMapping("/continue")
    public Geojson continueGeoJson(@RequestBody File geojson) {
        Geojson geojsonTocontinue = fileMapper.apply(geojson);

        return geoJsonContinuerService.continueGeojson(geojsonTocontinue);
    }
}

