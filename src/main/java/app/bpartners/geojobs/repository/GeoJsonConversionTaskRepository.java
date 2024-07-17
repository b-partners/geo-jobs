package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.GeoJsonConversionTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoJsonConversionTaskRepository
    extends JpaRepository<GeoJsonConversionTask, String> {
  Optional<GeoJsonConversionTask> findByJobId(String jobId);
}
