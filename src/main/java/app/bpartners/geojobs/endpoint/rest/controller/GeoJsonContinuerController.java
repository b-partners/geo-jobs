package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.mapper.FileMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
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
    private final BucketComponent bucketComponent;

    @GetMapping("/continue")
    public String continueGeoJson(@RequestBody File geojsonInput) {
        var geojsonToContinue = fileMapper.apply(geojsonInput);
        var result = geoJsonContinuerService.continueGeojson(geojsonToContinue);

        var fileName = "geojson-result-" + System.currentTimeMillis() + ".geojson";
        var tempPath = System.getProperty("java.io.tmpdir") + "/" + fileName;

        result.saveAsFile(tempPath);
        File outputFile = new File(tempPath);

        String s3Key = "geojson/results/" + fileName;
        bucketComponent.upload(outputFile, s3Key);

        return bucketComponent.presign(s3Key);
    }
}
