package app.bpartners.geojobs.endpoint.rest.security;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.api.DetectionApi;
import app.bpartners.geojobs.endpoint.rest.api.TilingApi;
import app.bpartners.geojobs.endpoint.rest.client.ApiClient;
import app.bpartners.geojobs.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.endpoint.rest.controller.ZoneDetectionController;
import app.bpartners.geojobs.endpoint.rest.controller.ZoneTilingController;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;
import static app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticator.APIKEY_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setup(){
        doNothing().when(zoneDetectionJobValidator).accept(any());
        when(zoneDetectionJobService.fireTasks(any())).thenReturn(null);
        when(zoneDetectionJobMapper.toRest(any(), any())).thenReturn(null);
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
    void community_cannot_detect_if_type_is_not_authorized(){
        setupClientWithApiKey("community1_key");
        var error = assertThrows(ApiException.class, ()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                List.of(PATHWAY, TREE)
            ));
        });
        assertTrue(error.getMessage().contains("call failed with: 403"));
    }

    @Test
    void community_cannot_detect_if_one_object_type_is_not_authorized(){
        setupClientWithApiKey("community2_key");
        var error = assertThrows(ApiException.class, ()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                List.of(PATHWAY, ROOF)
            ));
        });
        assertTrue(error.getMessage().contains("call failed with: 403"));
    }

    @Test
    void community1_can_detect_with_authorized_object_type(){
        setupClientWithApiKey("community1_key");
        assertDoesNotThrow(()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                List.of(ROOF, POOL)
            ));
        });
        assertDoesNotThrow(()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                List.of(POOL)
            ));
        });
    }

    @Test
    void community2_can_detect_with_authorized_object_type(){
        setupClientWithApiKey("community2_key");
        assertDoesNotThrow(()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                List.of(PATHWAY)
            ));
        });
    }

    @Test
    void community_cannot_detect_not_authorized_zoneName(){
        setupClientWithApiKey("community1_key");
        assertDoesNotThrow(()->{
            detectionApi.processZDJ("dummy", asZoneDetectionJob(
                    List.of(PATHWAY)
            ));
        });

        setupClientWithApiKey("community2_key");
        assertDoesNotThrow(()->{
            tilingApi.tileZone(asZoneTilingJob("not_authorized_zone_name"));
        });
    }

    private List<DetectableObjectConfiguration> asZoneDetectionJob(List<DetectableObjectType> detectableObjectTypes){
        return detectableObjectTypes.stream().map(detectableObjectType -> {
            var detectableObjectConfiguration = new DetectableObjectConfiguration();
            detectableObjectConfiguration.setType(detectableObjectType);
            return detectableObjectConfiguration;
        }).toList();
    }

    private CreateZoneTilingJob asZoneTilingJob(String zoneName) {
        var zoneTilingJob = new CreateZoneTilingJob();
        zoneTilingJob.setZoneName(zoneName);
        return zoneTilingJob;
    }
}