package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geo.detection.DetectedTile;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectedTileRepository extends TaskRepository<DetectedTile> {}
