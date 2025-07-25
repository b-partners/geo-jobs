package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.postprocessing.mapper.FileMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@AllArgsConstructor
@RestController
public class GeoJsonContinuerController {
    private final GeoJsonContinuerService geoJsonContinuerService;
    private final FileMapper fileMapper;
    private final BucketComponent bucketComponent;

    @PostMapping("/continue")
    public String continueGeoJson(@RequestBody File geojsonInput) throws IOException {
        var result = geoJsonContinuerService.continueGeojson(geojsonInput);

        var prefix = "geojson-result-";
        var suffix = ".geojson";
        var tempFile = File.createTempFile(prefix, suffix);
        Files.writeString(tempFile.toPath(), result.toString());

        var bucketKey = "geojson/results/" + tempFile.getName();
        bucketComponent.upload(tempFile, bucketKey);

        return bucketComponent.presign(bucketKey);
    }
}
