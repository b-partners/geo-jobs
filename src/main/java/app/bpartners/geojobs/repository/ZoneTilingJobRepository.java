package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneTilingJobRepository extends JpaRepository<ZoneTilingJob, String> {}
