package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_DEGREE;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.CreateApiKey;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizedZone;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiKeyMapper {
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;

  public List<CommunityAuthorization> toCommunityAuthorization(List<CreateApiKey> createApiKeys) {
    return createApiKeys.stream()
        .map(
            createApiKey -> {
              var newCommunityId = randomUUID().toString();
              return toCommunityAuthorization(createApiKey, newCommunityId);
            })
        .toList();
  }

  private CommunityAuthorization toCommunityAuthorization(
      CreateApiKey createApiKey, String newCommunityId) {
    return CommunityAuthorization.builder()
        .id(newCommunityId)
        .apiKey(randomUUID().toString())
        .name(createApiKey.getConsumerName())
        .email(createApiKey.getConsumerEmail())
        .detectableObjectTypes(
            createApiKey.getDetectableObjectTypes().stream()
                .map(
                    detectableObjectType ->
                        toCommunityDetectableObjectType(newCommunityId, detectableObjectType))
                .toList())
        .maxSurface(createApiKey.getMaxSurface().doubleValue())
        .maxSurfaceUnit(SQUARE_DEGREE)
        .authorizedZones(toCommunityAuthorizedZone(createApiKey, newCommunityId))
        .build();
  }

  private List<CommunityAuthorizedZone> toCommunityAuthorizedZone(
      CreateApiKey createApiKey, String newCommunityId) {
    return createApiKey.getAuthorizedZones().stream()
        .map(
            authorizedZone ->
                CommunityAuthorizedZone.builder()
                    .id(randomUUID().toString())
                    .communityAuthorizationId(newCommunityId)
                    .name(authorizedZone.getName())
                    .multiPolygon(authorizedZone.getZone())
                    .build())
        .toList();
  }

  private CommunityDetectableObjectType toCommunityDetectableObjectType(
      String newCommunityId, DetectableObjectType detectableObjectType) {
    return CommunityDetectableObjectType.builder()
        .id(randomUUID().toString())
        .communityAuthorizationId(newCommunityId)
        .type(detectableObjectTypeMapper.toDomain(detectableObjectType))
        .build();
  }
}
