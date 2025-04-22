package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionAddressConversionTaskRepository
    extends TaskRepository<DetectionAddressConversionTask> {}
