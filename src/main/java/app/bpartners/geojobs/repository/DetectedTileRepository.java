package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectedTileRepository extends JpaRepository<MachineDetectedTile, String> {
  List<MachineDetectedTile> findAllByZdjJobId(String id);

  List<MachineDetectedTile> findAllByParcelId(String parcelId);

  @Query(
      value = "select * from get_tiles_without_detected_object(:zoneDetectionJobId)",
      nativeQuery = true)
  List<MachineDetectedTile> findAllInDoubtTilesWithoutObjectByZdjJobId(
      @Param("zoneDetectionJobId") String zoneDetectionJobId);

  @Query(
      value = "select * from get_in_doubt_detected_tiles(:zoneDetectionJobId, :isGreater)",
      nativeQuery = true)
  List<MachineDetectedTile> findAllInDoubtByZdjJobIdGreaterThan(
      @Param("zoneDetectionJobId") String zoneDetectionJobId,
      @Param("isGreater") Boolean isGreater);
}
