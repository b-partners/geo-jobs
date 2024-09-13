package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import org.junit.jupiter.api.Test;

class FullDetectionOwnerAuthorizerTest {
  FullDetectionOwnerAuthorizer subject = new FullDetectionOwnerAuthorizer();

  @Test
  void should_accept_if_the_given_community_is_the_owner() {
    var communityId = randomUUID().toString();
    assertDoesNotThrow(
        () ->
            subject.accept(
                CommunityAuthorization.builder().id(communityId).build(),
                FullDetection.builder().communityOwnerId(communityId).build()));
  }

  @Test
  void should_not_accept_if_the_given_community_is_the_owner() {
    var communityAuthorization =
        CommunityAuthorization.builder().id(randomUUID().toString()).build();
    var fullDetection = FullDetection.builder().communityOwnerId(randomUUID().toString()).build();

    assertThrows(
        ForbiddenException.class, () -> subject.accept(communityAuthorization, fullDetection));
  }
}
