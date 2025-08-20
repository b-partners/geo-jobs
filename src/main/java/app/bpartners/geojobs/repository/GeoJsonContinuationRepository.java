package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonContinuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoJsonContinuationRepository extends JpaRepository<GeoJsonContinuation, String> {}
