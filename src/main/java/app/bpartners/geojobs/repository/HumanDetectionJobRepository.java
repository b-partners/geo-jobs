package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HumanDetectionJobRepository extends JpaRepository<HumanDetectionJob, String> {}
