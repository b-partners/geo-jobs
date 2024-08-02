package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotationRetrievingTaskRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationRetrievingTaskService {
  private final AnnotationRetrievingTaskRepository repository;

  public List<AnnotationRetrievingTask> getByRetrievingJobId(String jobId) {
    return repository.findByJobId(jobId);
  }

  public AnnotationRetrievingTask getByAnnotationTaskId(String annotationTaskId) {
    return repository
        .findByAnnotationTaskId(annotationTaskId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No retrieving task found for annotation task id=" + annotationTaskId));
  }

  public List<AnnotationRetrievingTask> saveAll(List<AnnotationRetrievingTask> toSave) {
    return repository.saveAll(toSave);
  }
}
