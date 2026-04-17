package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.CommunityAuthorizationApiKeyRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunityAuthorizationApiKeyService {
  private final RevokedApiKeyRepository revokedApiKeyRepository;
  private final CommunityAuthorizationApiKeyRepository communityAuthorizationApiKeyRepository;

  @Transactional
  public RevokedApiKey revokeApiKey(CommunityAuthorizationApiKey communityAuthorizationApiKey) {
    var optionalRevokedApiKey =
        revokedApiKeyRepository.findByRevokedApiKeyValue(
            communityAuthorizationApiKey.getKeyValue());

    if (optionalRevokedApiKey.isPresent()) {
      throw new IllegalStateException("ApiKey already revoked");
    }

    RevokedApiKey revokedApiKey =
        RevokedApiKey.builder()
            .id(randomUUID().toString())
            .revokedApiKeyValue(communityAuthorizationApiKey.getKeyValue())
            .communityOwnerId(communityAuthorizationApiKey.getCommunityOwnerId())
            .revokedAt(now())
            .build();

    revokedApiKeyRepository.save(revokedApiKey);
    communityAuthorizationApiKeyRepository.deleteById(communityAuthorizationApiKey.getId());

    return revokedApiKey;
  }
}
