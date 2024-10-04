package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RevokedApiKeyRepository extends JpaRepository<RevokedApiKey, String> {
    Optional<RevokedApiKey> findByApiKey(String apiKey);
}
