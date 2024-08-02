package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotationRetrievingJobRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationRetrievingJobService {
  private final AnnotationRetrievingJobRepository repository;

  public List<AnnotationRetrievingJob> getByDetectionJobId(String id) {
    return repository.findByDetectionJobId(id);
  }

  public AnnotationRetrievingJob getByAnnotationJobId(String id) {
    return repository
        .findByAnnotationJobId(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No Annotation retrieving job found for annotation job id=" + id));
  }

  public AnnotationRetrievingJob getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new NotFoundException("Annotation retrieving job id=" + id + " is not found"));
  }

  public List<AnnotationRetrievingJob> saveAll(List<AnnotationRetrievingJob> toSave) {
    return repository.saveAll(toSave);
  }
}
