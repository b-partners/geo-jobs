package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoJsonConversionJobRepository extends JobRepository<GeoJsonConversionJob> {
  Optional<GeoJsonConversionJob> findByZoneDetectionJobId(String jobId);
}
