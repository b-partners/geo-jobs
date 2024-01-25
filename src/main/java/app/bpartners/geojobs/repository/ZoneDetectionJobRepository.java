package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.ZoneDetectionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneDetectionJobRepository extends JpaRepository<ZoneDetectionJob, String> {}
