package app.bpartners.geojobs.endpoint.rest.security;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.TilingApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.util.List;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;
import static app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticator.APIKEY_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CommunityAuthenticatedAccessIT extends FacadeIT {
    TilingApi tilingApi;
    DetectionApi detectionApi;

    @Autowired
    ObjectMapper om;

    @LocalServerPort
    private int port;

    @MockBean
    ZoneDetectionJobValidator zoneDetectionJobValidator;

    @MockBean
    ZoneDetectionJobService zoneDetectionJobService;

    @MockBean
    ZoneDetectionJobMapper zoneDetectionJobMapper;

    @MockBean
    ZoneTilingJobMapper zoneTilingJobMapper;

    @MockBean
    ZoneTilingJobService zoneTilingJobService;

    ZoneDetectionJob expectedZoneDetectionJob = new ZoneDetectionJob();
    ZoneTilingJob expectedZoneTilingJob = new ZoneTilingJob();

    @BeforeEach
    void setup(){
        doNothing().when(zoneDetectionJobValidator).accept(any());
        when(zoneDetectionJobMapper.toRest(any(), any())).thenReturn(expectedZoneDetectionJob);
        when(zoneTilingJobMapper.toRest(any(), any())).thenReturn(expectedZoneTilingJob);
        when(zoneDetectionJobService.fireTasks(any())).thenReturn(null);
        when(zoneTilingJobService.create(any(), any())).thenReturn(null);
        when(zoneTilingJobMapper.toDomain(any())).thenReturn(new app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob());
    }

    void setupClientWithApiKey(String apiKey) {
        var authenticatedClient = new ApiClient();
        authenticatedClient.setRequestInterceptor(
                builder -> builder.header(APIKEY_HEADER_NAME, apiKey));
        authenticatedClient.setScheme("http");
        authenticatedClient.setPort(port);
        authenticatedClient.setObjectMapper(om);

        tilingApi = new TilingApi(authenticatedClient);
        detectionApi = new DetectionApi(authenticatedClient);
    }

    @Test
    void community_cannot_detect_if_type_is_not_authorized_object_type(){
        setupClientWithApiKey("community1_key");
        var error = assertThrows(ApiException.class, ()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                List.of(PATHWAY, TREE)
            ));
        });
        assertTrue(error.getMessage().contains("TREE"));
        assertTrue(error.getMessage().contains("PATHWAY"));
    }

    @Test
    void community_cannot_detect_if_one_object_type_is_not_authorized(){
        setupClientWithApiKey("community2_key");
        var error = assertThrows(ApiException.class, ()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                List.of(PATHWAY, ROOF)
            ));
        });
        assertTrue(error.getMessage().contains("ROOF"));
        assertFalse(error.getMessage().contains("PATHWAY"));
    }

    @Test
    void community_can_detect_with_correct_authorization() throws ApiException {
        setupClientWithApiKey("community1_key");
        var actualWithPoolAndRoofObject = detectionApi.processZDJ("dummy", asZoneDetectionJob(
            List.of(ROOF, POOL)
        ));
        assertEquals(expectedZoneDetectionJob, actualWithPoolAndRoofObject);
        var actualWithPoolObject = detectionApi.processZDJ("dummy", asZoneDetectionJob(
            List.of(POOL)
        ));
        assertEquals(expectedZoneDetectionJob, actualWithPoolObject);

        setupClientWithApiKey("community2_key");
        var actualWithPathwayObject = detectionApi.processZDJ("dummy", asZoneDetectionJob(
            List.of(PATHWAY)
        ));
        assertEquals(expectedZoneDetectionJob, actualWithPathwayObject);
    }

    @Test
    void community_cannot_do_tiling_with_not_authorized_zoneName(){
        setupClientWithApiKey("community1_key");
        var community1Error = assertThrows(ApiException.class, ()->{
            tilingApi.tileZone(asZoneTilingJob("not_authorized_zone_name"));
        });
        assertTrue(community1Error.getMessage().contains("not_authorized_zone_name"));

        setupClientWithApiKey("community2_key");
        var community2Error = assertThrows(ApiException.class, ()->{
            tilingApi.tileZone(asZoneTilingJob("not_authorized_zone_name"));
        });
        assertTrue(community2Error.getMessage().contains("not_authorized_zone_name"));
    }

    @Test
    void community_can_do_tiling_with_authorized_zone_name() throws ApiException {
        setupClientWithApiKey("community1_key");
        var actualWithZoneName1 = tilingApi.tileZone(asZoneTilingJob("zoneName1"));
        assertEquals(expectedZoneTilingJob, actualWithZoneName1);

        setupClientWithApiKey("community2_key");
        var actualWithZoneName2 = tilingApi.tileZone(asZoneTilingJob("zoneName2"));
        assertEquals(expectedZoneTilingJob, actualWithZoneName2);
        var actualWithZoneName3 = tilingApi.tileZone(asZoneTilingJob("zoneName3"));
        assertEquals(expectedZoneTilingJob, actualWithZoneName3);
    }

    private List<DetectableObjectConfiguration> asZoneDetectionJob(List<DetectableObjectType> detectableObjectTypes){
        return detectableObjectTypes.stream().map(detectableObjectType ->
            new DetectableObjectConfiguration()
                .type(detectableObjectType)
                .confidence(BigDecimal.TEN)
                .bucketStorageName("dummy_bucket_storage_name")
        ).toList();
    }

    private CreateZoneTilingJob asZoneTilingJob(String zoneName) {
        return new CreateZoneTilingJob()
            .geoServerUrl("http://dummy_url")
            .zoneName(zoneName)
            .features(List.of());
    }
}