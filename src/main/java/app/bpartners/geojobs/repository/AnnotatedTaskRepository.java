package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.annotator.AnnotatedTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotatedTaskRepository extends JpaRepository<AnnotatedTask, String> {
  Optional<AnnotatedTask> findByCreateAnnotatedTaskId(String createAnnotatedTaskId);
}
