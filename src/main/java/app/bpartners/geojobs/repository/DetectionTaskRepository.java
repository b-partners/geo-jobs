package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geo.detection.DetectionTask;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionTaskRepository extends TaskRepository<DetectionTask> {}
