package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.RevokeApiKeyResponse;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevokedApiKeyService {
  private final RevokedApiKeyRepository repository;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final CommunityAuthorizationApiKeyService communityAuthorizationApiKeyService;

  /**
   * @deprecated Uses the API key stored in the community authorization table, which is part of a
   *     deprecated API key system and should no longer be used.
   */
  @Deprecated
  @Transactional
  public RevokeApiKeyResponse revokeCommunityLatestApiKey(
      CommunityAuthorization communityAuthorization) {
    if (communityAuthorization.isApiKeyRevoked()) {
      throw new BadRequestException("Cannot revoke apikey as it is already revoked");
    }

    revokeRawApiKey(communityAuthorization);

    return new RevokeApiKeyResponse().message("Your API key has been successfully revoked");
  }

  @Transactional
  public RevokeApiKeyResponse revokeCommunityApiKey(
      CommunityAuthorization authenticatedAuthorization, String apiKeyValue) {
    Optional<CommunityAuthorization> optionalTargetAuthorization =
        communityAuthRepository.findByApiKey(apiKeyValue);

    if (optionalTargetAuthorization.isEmpty()) {
      throw new NotFoundException("The API key " + hide(apiKeyValue) + " was not found.");
    }

    CommunityAuthorization targetAuthorization = optionalTargetAuthorization.get();

    if (!targetAuthorization.getId().equals(authenticatedAuthorization.getId())
        && ROLE_ADMIN.equals(authenticatedAuthorization.getRole())) {
      throw new AccessDeniedException("Users can only revoke their own API keys.");
    }
    List<RevokedApiKey> revokedApiKeys =
        revokeDomainCommunityApiKey(targetAuthorization, apiKeyValue);

    if (revokedApiKeys.isEmpty()) {
      revokeRawApiKey(targetAuthorization);
    }

    return new RevokeApiKeyResponse()
        .message(String.format("The API key %s has been successfully revoked", hide(apiKeyValue)));
  }

  @NotNull
  private List<RevokedApiKey> revokeDomainCommunityApiKey(
      CommunityAuthorization communityAuthorization, String apiKeyValue) {
    List<CommunityAuthorizationApiKey> apiKeys = communityAuthorization.getApiKeys();

    return apiKeys.stream()
        .filter(key -> apiKeyValue.equals(key.getKeyValue()))
        .map(communityAuthorizationApiKeyService::revokeApiKey)
        .toList();
  }

  /**
   * @deprecated Uses the API key stored in the community authorization table, which is part of a
   *     deprecated API key system and should no longer be used.
   */
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

  static String hide(String apiKey) {
    int keyLength = apiKey.length();
    int hideRange = keyLength / (keyLength / 6);
    String shownPart = apiKey.substring(hideRange, (keyLength - hideRange));
    String hider = "*".repeat(hideRange);

    return hider + shownPart + hider;
  }
}
