package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import org.springframework.stereotype.Repository;

@Repository
public interface TilingTaskRepository extends TaskRepository<TilingTask> {}
