package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectableObjectConfigurationRepository extends JpaRepository<DetectableObjectConfiguration, String> {
    List<DetectableObjectConfiguration> findAllByDetectionJobId(String jobId);
}
