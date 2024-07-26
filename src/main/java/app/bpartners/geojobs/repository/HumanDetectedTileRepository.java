package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HumanDetectedTileRepository extends JpaRepository<HumanDetectedTile, String> {
  List<HumanDetectedTile> findByJobId(String jobId);
}
