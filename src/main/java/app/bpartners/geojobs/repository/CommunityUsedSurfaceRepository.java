package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.CommunityUsedSurface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityUsedSurfaceRepository extends JpaRepository<CommunityUsedSurface, String> {
    List<CommunityUsedSurface> findByCommunityAuthorization_ApiKeyOrderByUsageDatetimeDesc(String apiKey);
}
