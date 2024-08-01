package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotationRetrievingTaskRepository
    extends JpaRepository<AnnotationRetrievingTask, String> {
  Optional<AnnotationRetrievingTask> findByAnnotationTaskId(String annotationTaskId);

  List<AnnotationRetrievingTask> findByJobId(String jobId);
}
