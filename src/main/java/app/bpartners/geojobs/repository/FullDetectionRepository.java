package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.FullDetection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FullDetectionRepository extends JpaRepository<FullDetection, String> {
  FullDetection findByEndToEndId(String endToEndId);

  FullDetection findByZtjId(String ztjId);
}
