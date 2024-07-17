package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.GeoJsonConversionTask;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GeoJsonConversionTaskService {
  private final GeoJsonConversionTaskRepository repository;

  public GeoJsonConversionTask save(GeoJsonConversionTask task) {
    return repository.save(task);
  }

  public GeoJsonConversionTask getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("GeoJson task.id=" + id + " is not found"));
  }

  public GeoJsonConversionTask getByJobId(String jobId) {
    return repository
        .findByJobId(jobId)
        .orElseThrow(
            () ->
                new NotFoundException("GeoJson task linked to job.id=" + jobId + " is not found"));
  }
}
