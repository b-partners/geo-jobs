package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelDetectionJobRepository extends JpaRepository<ParcelDetectionJob, String> {}
