package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.datastructure.ListGrouper;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.consumer.PolygonFusionRequested;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.service.PolygonContinue.PolygonContinueService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class PolygonFusionMultiControllerIT extends FacadeIT {

    @Autowired
    private GeoJsonLoader geoJsonLoader;

    @Test
    void testPolygonFusionAndPrintMergedPath() throws Exception {
        File fakeGeoJson = new File("src/test/resources/fake.geojson");
        byte[] geojsonContent = Files.readAllBytes(fakeGeoJson.toPath());
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "fake.geojson", "application/geo+json", geojsonContent
        );

        BuildingApi buildingApi = new BuildingApi() {};
        GeometryConverter geometryConverter = new GeometryConverter(buildingApi);

        BucketComponent bucketComponent = new BucketComponent(null) {
            @Override
            public FileHash upload(File file, String bucketKey) {
                return new FileHash(null, "dummy-hash");
            }

            @Override
            public String presign(String bucketKey) {
                return "file://" + bucketKey;
            }
        };

        ObjectMapper objectMapper = new ObjectMapper();
        EventBridgeClient eventBridgeClient = mock(EventBridgeClient.class);
        String eventBusName = "default";

        ListGrouper<PolygonFusionRequested> listGrouper = new ListGrouper<>() {
            @Override
            public List<List<PolygonFusionRequested>> apply(List<PolygonFusionRequested> items, Integer size) {
                List<List<PolygonFusionRequested>> grouped = new ArrayList<>();
                grouped.add(new ArrayList<>(items));
                return grouped;
            }
        };

        EventProducer<PolygonFusionRequested> eventProducer = new EventProducer<>(
                objectMapper,
                eventBridgeClient,
                eventBusName,
                listGrouper
        );

        PolygonContinueService service = new PolygonContinueService(
                geometryConverter,
                geoJsonLoader,
                bucketComponent,
                eventProducer
        );

        Map<String, String> result = service.fusionnerPolygones(
                mockFile,
                "fake-bucket",
                "output/fused.geojson"
        );
        System.out.println("Chemin du polygon fusionné : " + result.get("localPath"));
        assertNotNull(result.get("localPath"));
        assertNotNull(result.get("url"));
    }

    @TestConfiguration
    static class TestBeansConfig {
        @Bean
        public GeoJsonLoader geoJsonLoader() {
            return new GeoJsonLoader();
        }
    }
}
