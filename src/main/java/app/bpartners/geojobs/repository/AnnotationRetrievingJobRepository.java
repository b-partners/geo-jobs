package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotationRetrievingJobRepository
    extends JpaRepository<AnnotationRetrievingJob, String> {
  List<AnnotationRetrievingJob> findByDetectionJobId(String detectionJobId);

  Optional<AnnotationRetrievingJob> findByAnnotationJobId(String id);
}
