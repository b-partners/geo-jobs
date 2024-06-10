package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.model.CommunityAuthorizationDetails;

public interface CommunityAuthorizationDetailsRepository {
  CommunityAuthorizationDetails findByApiKey(String apiKey);

  boolean existsByApiKey(String apiKey);
}
