package app.bpartners.geojobs.repository.impl;

import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class CommunityAuthorizationDetailsRepositoryImpl
    implements CommunityAuthorizationDetailsRepository {
  public static final String COMMUNITY_AUTHORIZATION_DETAILS_ENV_KEY = "${community.auth.details}";
  private final List<CommunityAuthorizationDetails> communityAuthorizationDetails;

  public CommunityAuthorizationDetailsRepositoryImpl(
      @Value(COMMUNITY_AUTHORIZATION_DETAILS_ENV_KEY) String communityAuthorizationDetails,
      ObjectMapper om)
      throws JsonProcessingException {
    this.communityAuthorizationDetails =
        om.readValue(communityAuthorizationDetails, new TypeReference<>() {});
  }

  @Override
  public CommunityAuthorizationDetails findByApiKey(String apiKey) {
    return communityAuthorizationDetails.stream()
        .filter(cad -> cad.apiKey().equals(apiKey))
        .findFirst()
        .orElseThrow(ForbiddenException::new);
  }

  @Override
  public boolean existsByApiKey(String apiKey) {
    return communityAuthorizationDetails.stream().anyMatch(cad -> cad.apiKey().equals(apiKey));
  }
}
