package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.repository.model.DummyUuid;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@PojaGenerated
@Repository
public interface DummyUuidRepository extends JpaRepository<DummyUuid, String> {

  @Override
  Optional<DummyUuid> findById(String id);
}
