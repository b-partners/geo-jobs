package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.RevokeApiKeyResponse;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokedApiKeyService {
  private final RevokedApiKeyRepository repository;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final CommunityAuthorizationApiKeyService communityAuthorizationApiKeyService;

  @Transactional
  public RevokeApiKeyResponse revokeCommunityApiKey(CommunityAuthorization communityAuthorization) {
    if (communityAuthorization.isApiKeyRevoked()) {
      throw new BadRequestException("Cannot revoke apikey as it is already revoked");
    }

    revokeRawApiKey(communityAuthorization);

    return new RevokeApiKeyResponse().message("Your API key has been successfully revoked");
  }

  public RevokeApiKeyResponse revokeCommunityApiKey(
      CommunityAuthorization communityAuthorization, String apiKeyValue) {
    List<CommunityAuthorizationApiKey> apiKeys = communityAuthorization.getApiKeys();

    List<RevokedApiKey> revokedApiKeys =
        apiKeys.stream()
            .filter(key -> apiKeyValue.equals(key.getKeyValue()))
            .map(communityAuthorizationApiKeyService::revokeApiKey)
            .toList();

    if (revokedApiKeys.isEmpty()) {
      Optional<CommunityAuthorization> optionalCommunityAuthorization =
          communityAuthRepository.findByApiKey(apiKeyValue);

      if (optionalCommunityAuthorization.isEmpty()) {
        throw new BadRequestException(
            "The user "
                + communityAuthorization.getEmail()
                + " does not have an API key with the value "
                + apiKeyValue);
      }

      revokeRawApiKey(optionalCommunityAuthorization.get());
    }

    return new RevokeApiKeyResponse()
        .message(String.format("Your API %s key has been successfully revoked", apiKeyValue));
  }

  @Deprecated
  private void revokeRawApiKey(CommunityAuthorization communityAuthorization) {
    var revokedApiKey =
        RevokedApiKey.builder()
            .id(randomUUID().toString())
            .revokedAt(now())
            .revokedApiKeyValue(communityAuthorization.getApiKey())
            .communityOwnerId(communityAuthorization.getId())
            .build();

    communityAuthorization.setApiKeyRevoked(true);
    repository.save(revokedApiKey);
    communityAuthRepository.save(communityAuthorization);
  }
}
