package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.datastructure.ListGrouper;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.consumer.PolygonContinueRequested;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.PolygonContinueRepository;
import app.bpartners.geojobs.service.PolygonContinue.PolygonContinueService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolygonContinueControllerIT extends FacadeIT {

    @Autowired
    private GeoJsonLoader geoJsonLoader;

    @Mock
    private PolygonContinueRepository repository;

    @Test
    void testPolygonContinueAndPrintMergedPath() throws Exception {
        File initialGeoJson = new File("src/test/resources/initialPolygons.geojson");
        byte[] geojsonContent = Files.readAllBytes(initialGeoJson.toPath());
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "initialPolygons.geojson", "application/geo+json", geojsonContent
        );
        var geometryConverter = new GeometryConverter(new BuildingApi());

        var bucketComponent = new BucketComponent(null) {
            @Override
            public FileHash upload(File file, String bucketKey) {
                return new FileHash(null, "dummy-hash");
            }
            @Override
            public String presign(String bucketKey) {
                return "file://" + bucketKey;
            }
        };

        var mockClient = mock(EventBridgeClient.class);
        PutEventsResponse initialPolygonsResponse = PutEventsResponse.builder()
                .entries(PutEventsResultEntry.builder().eventId("dummy-event-id").build())
                .build();

        when(mockClient.putEvents(any(PutEventsRequest.class))).thenReturn(initialPolygonsResponse);

        EventProducer<PolygonContinueRequested> eventProducer = new EventProducer<>(
                new ObjectMapper(), mockClient, "default", new ListGrouper<>() {}
        );

        PolygonContinueService service = new PolygonContinueService(
                geometryConverter,
                geoJsonLoader,
                bucketComponent,
                eventProducer,
                repository
        );


        Map<String, String> result = service.PolygonsContinueAsync(mockFile);


        System.out.println("Path of the continued polygon: " + result.get("localPath"));
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