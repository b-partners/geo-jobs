package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import java.util.Optional;

public interface CommunityAuthorizationDetailsRepository {
  Optional<CommunityAuthorizationDetails> findById(String id);

  boolean existsByApiKey(String apiKey);
}
