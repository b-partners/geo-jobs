package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.RevokeApiKeyResponse;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RevokedApiKeyServiceTest {
  RevokedApiKeyRepository revokedApiKeyRepositoryMock = mock();
  CommunityAuthorizationRepository communityAuthRepositoryMock = mock();
  CommunityAuthorizationApiKeyService communityAuthorizationApiKeyService = mock();
  RevokedApiKeyService subject =
      new RevokedApiKeyService(
          revokedApiKeyRepositoryMock,
          communityAuthRepositoryMock,
          communityAuthorizationApiKeyService);

  @Test
  void revoke_community_api_key_ok() {
    var communityAuthorization = communityAuthorizationWithApiKeys();
    var keyToRevoke = communityAuthorization.getApiKeys().getFirst();
    var revokedApiKey = revokedApiKey(keyToRevoke);
    var expected =
        new RevokeApiKeyResponse()
            .message(
                "Your API key " + keyToRevoke.getKeyValue() + " has been successfully revoked");
    when(communityAuthorizationApiKeyService.revokeApiKey(keyToRevoke)).thenReturn(revokedApiKey);

    var actual = subject.revokeCommunityApiKey(communityAuthorization, keyToRevoke.getKeyValue());

    assertEquals(expected, actual);
  }

  @Test
  void revoke_community_raw_api_key_ok() {
    var communityAuthorization = communityAuthorization(false);
    var keyToRevoke = communityAuthorization.getApiKey();
    var expected =
        new RevokeApiKeyResponse()
            .message("Your API key " + keyToRevoke + " has been successfully revoked");
    when(revokedApiKeyRepositoryMock.save(any(RevokedApiKey.class))).thenReturn(mock());
    when(communityAuthRepositoryMock.save(communityAuthorization)).thenReturn(mock());
    when(communityAuthRepositoryMock.findByApiKey(keyToRevoke))
        .thenReturn(Optional.of(communityAuthorization));

    var actual = subject.revokeCommunityApiKey(communityAuthorization, keyToRevoke);

    assertEquals(expected, actual);
  }

  @Test
  void bad_request_when_api_key_not_found() {
    var communityAuthorization = communityAuthorization(false);
    var keyToRevoke = communityAuthorization.getApiKey();
    when(communityAuthRepositoryMock.findByApiKey(keyToRevoke)).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.revokeCommunityApiKey(communityAuthorization, keyToRevoke));

    assertEquals(
        "The user "
            + communityAuthorization.getEmail()
            + " does not have an API key with the value "
            + keyToRevoke,
        actual.getMessage());
  }

  @Test
  void cannot_revoked_api_key_if_already_revoked() {
    var communityAuthorization = communityAuthorization(true);
    var error =
        assertThrows(
            BadRequestException.class, () -> subject.revokeCommunityApiKey(communityAuthorization));
    assertEquals("Cannot revoke apikey as it is already revoked", error.getMessage());
  }

  @Test
  void can_revoke_api_key_ok() {
    var communityAuthorization = communityAuthorization(false);
    var expected = new RevokeApiKeyResponse().message("Your API key has been successfully revoked");
    when(communityAuthRepositoryMock.save(any(CommunityAuthorization.class))).thenReturn(mock());
    when(revokedApiKeyRepositoryMock.save(any(RevokedApiKey.class))).thenReturn(mock());

    var actual = subject.revokeCommunityApiKey(communityAuthorization);

    assertEquals(expected, actual);
    verify(revokedApiKeyRepositoryMock, times(1)).save(any(RevokedApiKey.class));
    verify(communityAuthRepositoryMock, times(1)).save(communityAuthorization(true));
  }

  CommunityAuthorization communityAuthorization(boolean isApiKeyRevoked) {
    return CommunityAuthorization.builder()
        .id("communityId")
        .name("communityName")
        .apiKey("communityApiKey")
        .apiKeys(List.of())
        .email("myemail@gmail.com")
        .maxSurface(1_000)
        .isApiKeyRevoked(isApiKeyRevoked)
        .build();
  }

  static CommunityAuthorization communityAuthorizationWithApiKeys() {
    return CommunityAuthorization.builder()
        .id("communityId")
        .name("communityName")
        .apiKeys(List.of(communityAuthorizationApiKey(1), communityAuthorizationApiKey(2)))
        .email("myemail@gmail.com")
        .maxSurface(1_000)
        .isApiKeyRevoked(false)
        .build();
  }

  static CommunityAuthorizationApiKey communityAuthorizationApiKey(int number) {
    return CommunityAuthorizationApiKey.builder()
        .id("communityAuthorizationApiKeyId" + number)
        .communityOwnerId("communityId" + number)
        .keyValue("communityApiKeyValue" + number)
        .creationDatetime(now())
        .build();
  }

  static RevokedApiKey revokedApiKey(CommunityAuthorizationApiKey communityAuthorizationApiKey) {
    return RevokedApiKey.builder()
        .id(randomUUID().toString())
        .revokedApiKeyValue(communityAuthorizationApiKey.getKeyValue())
        .communityOwnerId(communityAuthorizationApiKey.getCommunityOwnerId())
        .revokedAt(now())
        .build();
  }
}
