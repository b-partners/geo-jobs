package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonRoadContinuation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeoJsonRoadContinuationRepository
    extends JpaRepository<GeoJsonRoadContinuation, String> {}
