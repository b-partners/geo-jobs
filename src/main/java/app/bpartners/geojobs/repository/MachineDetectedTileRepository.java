package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineDetectedTileRepository extends JpaRepository<MachineDetectedTile, String> {
  Long countByZdjJobId(String jobId);

  List<MachineDetectedTile> findAllByZdjJobId(String id, Pageable pageable);

  List<MachineDetectedTile> findAllByParcelId(String parcelId);

  @Query(
      value = "select * from get_tiles_without_detected_object(:zoneDetectionJobId)",
      nativeQuery = true)
  List<MachineDetectedTile> findAllInDoubtTilesWithoutObjectByZdjJobId(
      @Param("zoneDetectionJobId") String zoneDetectionJobId);

  @Query(
      value =
          "select * from get_in_doubt_detected_tiles(:zoneDetectionJobId,:minConfidenceForDelivery,"
              + " :isGreater)",
      nativeQuery = true)
  List<MachineDetectedTile> findAllInDoubtByZdjJobId(
      @Param("zoneDetectionJobId") String zoneDetectionJobId,
      @Param("minConfidenceForDelivery") double minConfidenceForDelivery,
      @Param("isGreater") Boolean isGreater);

  @Query(
      value =
          "select count(*) from"
              + " get_in_doubt_detected_tiles(:zoneDetectionJobId,:minConfidenceForDelivery, true)",
      nativeQuery = true)
  Long countInDoubtDetectedTileToDeliveryByZdjJobId(
      @Param("zoneDetectionJobId") String zoneDetectionJobId,
      @Param("minConfidenceForDelivery") double minConfidenceForDelivery);
}
