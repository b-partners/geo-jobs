package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnnotationRetrievingTaskRepository extends JpaRepository<AnnotationRetrievingTask, String> {
  Optional<AnnotationRetrievingTask> findByAnnotationTaskId(String annotationTaskId);
}
