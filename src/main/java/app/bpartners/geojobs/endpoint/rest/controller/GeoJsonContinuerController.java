package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@AllArgsConstructor
@RestController
public class GeoJsonContinuerController {
    private final GeoJsonContinuerService geoJsonContinuerService;
    private final BucketComponent bucketComponent;

    @PostMapping(value = "/continue")
    public String continueGeoJson(@RequestParam("file") MultipartFile file) throws IOException {
        File tempInput = File.createTempFile("geojson-input-", ".geojson");
        file.transferTo(tempInput);

        var result = geoJsonContinuerService.continueGeojson(tempInput);

        File tempOutput = File.createTempFile("geojson-result-", ".geojson");
        Files.writeString(tempOutput.toPath(), result.toString());

        String bucketKey = "geojson/results/" + tempOutput.getName();
        bucketComponent.upload(tempOutput, bucketKey);

        return bucketComponent.presign(bucketKey);
    }
}
