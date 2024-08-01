package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotationRetrievingTaskRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class AnnotationRetrievingTaskService {
  private final AnnotationRetrievingTaskRepository repository;

  public AnnotationRetrievingTask getByAnnotationTaskId(String annotationTaskId){
    return repository.findByAnnotationTaskId(annotationTaskId)
        .orElseThrow(()-> new NotFoundException("No retrieving task found for annotation task id="+annotationTaskId));
  }

  public AnnotationRetrievingTask save(AnnotationRetrievingTask toSave){
    return repository.save(toSave);
  }
}
