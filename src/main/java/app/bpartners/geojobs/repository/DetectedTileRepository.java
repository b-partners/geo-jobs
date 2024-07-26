package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectedTileRepository extends JpaRepository<MachineDetectedTile, String> {
  List<MachineDetectedTile> findAllByZdjJobId(String id);

  List<MachineDetectedTile> findAllByParcelId(String parcelId);
}
