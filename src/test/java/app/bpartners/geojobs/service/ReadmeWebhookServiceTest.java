package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.ReadmeWebhookService;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SingleUserInfo;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadmeWebhookServiceTest {
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock =
      mock(CommunityAuthorizationRepository.class);
  ReadmeWebhookService subject = new ReadmeWebhookService(communityAuthorizationRepositoryMock);

  @Test
  void retrieve_user_info_by_email_ok_use_repository() {
    var communityAuthorization = new CommunityAuthorization();
    when(communityAuthorizationRepositoryMock.findByEmail(any()))
        .thenReturn(Optional.of(communityAuthorization));

    var actual = subject.retrieveUserInfoByEmail("email@example.com");

    var expected = SingleUserInfo.builder().build();
    assertEquals(expected, actual);
  }
}
