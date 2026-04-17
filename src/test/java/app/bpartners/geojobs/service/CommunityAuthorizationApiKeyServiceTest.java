package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.CommunityAuthorizationApiKeyRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunityAuthorizationApiKeyServiceTest {
  private static final String KEY_VALUE = randomUUID().toString();
  private final RevokedApiKeyRepository revokedApiKeyRepository = mock();
  private final CommunityAuthorizationApiKeyRepository communityAuthorizationApiKeyRepository = mock();
  
  CommunityAuthorizationApiKeyService subject = new CommunityAuthorizationApiKeyService(revokedApiKeyRepository, communityAuthorizationApiKeyRepository);
  
  @Test
  void revoking_already_revoked_key_is_illegal() {
    CommunityAuthorizationApiKey apiKey = apiKey();
    when(revokedApiKeyRepository.findByRevokedApiKeyValue(apiKey.getKeyValue())).thenReturn(Optional.of(revokedApiKey()));

    var actual = assertThrows(IllegalStateException.class, () -> subject.revokeApiKey(apiKey));

    assertEquals("ApiKey already revoked", actual.getMessage());
  }

  @Test
  void key_revocation_integrity() {
    CommunityAuthorizationApiKey apiKey = apiKey();
    when(revokedApiKeyRepository.findByRevokedApiKeyValue(apiKey.getKeyValue())).thenReturn(Optional.empty());
    when(revokedApiKeyRepository.save(any())).thenReturn(revokedApiKey());
    doNothing().when(communityAuthorizationApiKeyRepository).deleteById(apiKey.getId());

    var actual = subject.revokeApiKey(apiKey);

    verify(revokedApiKeyRepository, times(1)).save(any());
    verify(communityAuthorizationApiKeyRepository, times(1)).deleteById(apiKey.getId());
    assertEquals(apiKey.getKeyValue(), actual.getRevokedApiKeyValue());
  }

  private static CommunityAuthorizationApiKey apiKey() {
    return CommunityAuthorizationApiKey.builder()
        .id(randomUUID().toString())
        .keyValue(KEY_VALUE)
        .build();
  }

  private static RevokedApiKey revokedApiKey() {
    return RevokedApiKey.builder()
        .revokedApiKeyValue(KEY_VALUE)
        .revokedAt(now())
        .build();
  }

}