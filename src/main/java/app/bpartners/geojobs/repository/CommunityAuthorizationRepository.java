package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.CommunityAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityAuthorizationRepository extends JpaRepository<CommunityAuthorization, String> {
  Optional<CommunityAuthorization> findByApiKey(String apiKey);
}