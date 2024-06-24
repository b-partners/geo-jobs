package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectedTileRepository extends JpaRepository<DetectedTile, String> {
  List<DetectedTile> findAllByZdjJobId(String id);

  List<DetectedTile> findAllByParcelId(String parcelId);
}
