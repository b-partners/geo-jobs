package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthentication;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticationFilter;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.authorizer.CommunityZoneDetectionJobProcessAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.impl.CommunityAuthorizationDetailsRepositoryImpl;
import app.bpartners.geojobs.repository.model.detection.DetectableType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class CommunityZoneDetectionJobProcessAuthorizerIT extends FacadeIT {
    private final MockedStatic<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilter =
            mockStatic(ApiKeyAuthenticationFilter.class);
    @MockBean CommunityAuthorizationDetailsRepositoryImpl communityAuthorizationDetailsRepository;

    @Autowired CommunityZoneDetectionJobProcessAuthorizer communityZoneDetectionJobProcessAuthorizer;

    @Test
    void should_accept_directly_admin_key() {
        useRole(ROLE_ADMIN);
        assertDoesNotThrow(
                () -> {
                    communityZoneDetectionJobProcessAuthorizer.accept(
                            "dummyJobId",
                            asDetectableObjectConfiguration(List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF))
                    );
                });
    }

    @Test
    void community_cannot_detect_not_authorized_object_type() {
        useRole(ROLE_COMMUNITY);
        when(communityAuthorizationDetailsRepository.findByApiKey(any()))
                .thenReturn(asCommunityAuthorizationDetails(List.of(POOL)));

        var error =
                assertThrows(
                        ForbiddenException.class,
                        () -> {
                            communityZoneDetectionJobProcessAuthorizer.accept(
                                    "dummyJobId",
                                    asDetectableObjectConfiguration(List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF)));
                        });
        assertTrue(error.getMessage().contains("PATHWAY"));
        assertTrue(error.getMessage().contains("ROOF"));
    }

    @Test
    void should_throws_forbidden_if_community_doesnt_have_access_to_one_of_payload_object_types() {
        useRole(ROLE_COMMUNITY);
        when(communityAuthorizationDetailsRepository.findByApiKey(any()))
                .thenReturn(asCommunityAuthorizationDetails(List.of(PATHWAY)));

        var error =
            assertThrows(
                ForbiddenException.class,
                () -> {
                    communityZoneDetectionJobProcessAuthorizer.accept(
                        "dummyJobId",
                        asDetectableObjectConfiguration(List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF)));
                });
        assertTrue(error.getMessage().contains("ROOF"));
    }

    @Test
    void should_accept_community_with_correct_permissions() {
        useRole(ROLE_COMMUNITY);
        when(communityAuthorizationDetailsRepository.findByApiKey(any()))
                .thenReturn(asCommunityAuthorizationDetails(List.of(PATHWAY, ROOF, POOL)));

        assertDoesNotThrow(
            () -> {
                communityZoneDetectionJobProcessAuthorizer.accept(
                    "dummyJobId",
                    asDetectableObjectConfiguration(List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF)));
                communityZoneDetectionJobProcessAuthorizer.accept(
                    "dummyJobId",
                    asDetectableObjectConfiguration(List.of(DetectableObjectType.PATHWAY, DetectableObjectType.ROOF, DetectableObjectType.POOL)));
            });
    }

    private CommunityAuthorizationDetails asCommunityAuthorizationDetails(
            List<DetectableType> detectableObjectTypes) {
        return new CommunityAuthorizationDetails(
                "dummy_id", "dummy_name", "dummy_name", List.of("dummy_zone_name"), detectableObjectTypes);
    }

    public List<DetectableObjectConfiguration> asDetectableObjectConfiguration(
            List<DetectableObjectType> detectableObjectTypes) {
        return detectableObjectTypes.stream()
                .map(
                        objetType -> new DetectableObjectConfiguration().type(objetType).confidence(BigDecimal.TEN)
                ).toList();
    }

    void useRole(Authority.Role role) {
        var apiKeyAuthentication =
                new ApiKeyAuthentication("dummy-api-key", Set.of(new Authority(role)));
        apiKeyAuthenticationFilter
                .when(ApiKeyAuthenticationFilter::getApiKeyAuthentication)
                .thenReturn(apiKeyAuthentication);
    }

    @AfterEach
    void cleanMock() {
        // to avoid creating multiple static mock registration
        apiKeyAuthenticationFilter.close();
    }
}