package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BucketServiceRoadContinuer {
    private final RoadContinuerService roadContinuerService;
    private final BucketComponent bucketComponent;

    public String getContinuedRoutePresignedUrl(String geoJsonRaw, TilingConf tilingConf) {
        try {
            Geojson continuedGeojson = roadContinuerService.continueRoute(geoJsonRaw, tilingConf);
            File tempOutputFile = File.createTempFile("continued-", ".geojson");
            tempOutputFile.deleteOnExit();
            continuedGeojson.saveAsFile(tempOutputFile.getAbsolutePath());
            String bucketKey = "continued-routes/" + UUID.randomUUID() + ".geojson";
            bucketComponent.upload(tempOutputFile, bucketKey);
            return bucketComponent.presign(bucketKey);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la continuation de la route", e);
        }
    }
}
