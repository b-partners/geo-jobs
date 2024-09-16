package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.FullDetection;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FullDetectionRepository extends JpaRepository<FullDetection, String> {
  Optional<FullDetection> findByEndToEndId(String endToEndId);

  Optional<FullDetection> findByZtjId(String ztjId);

  Optional<FullDetection> findByZdjId(String ztjId);

  List<FullDetection> findByCommunityOwnerId(String communityOwnerId, Pageable pageable);
}
