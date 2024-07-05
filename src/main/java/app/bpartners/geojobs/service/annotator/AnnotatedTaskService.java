package app.bpartners.geojobs.service.annotator;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotatedTaskRepository;
import app.bpartners.geojobs.repository.model.annotator.AnnotatedTask;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotatedTaskService {
  private final AnnotatedTaskRepository repository;

  public AnnotatedTask getByCreateAnnotatedTaskId(String id) {
    return repository
        .findByCreateAnnotatedTaskId(id)
        .orElseThrow(() -> new NotFoundException("Annotated task id=" + id + " is not found."));
  }

  public List<AnnotatedTask> saveAll(List<AnnotatedTask> tasks) {
    return repository.saveAll(tasks);
  }
}
