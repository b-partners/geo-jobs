package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ParcelService {

  private final TilingTaskRepository tilingTaskRepository;
  private final ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobRepository detectionJobRepository;

  @Transactional
  public List<Parcel> getParcelsByJobId(String jobId) {
    // TODO: refactor duplicated computing
    var zoneTilingJob = tilingJobRepository.findById(jobId);
    if (zoneTilingJob.isPresent()) {
      List<TilingTask> duplicatedTilingTasks =
          tilingTaskRepository.findAllByJobId(jobId).stream()
              .map(
                  task -> {
                    boolean hasSameStatuses = true;
                    boolean hasSameTile = true;
                    return task.duplicate(
                        task.getId(),
                        task.getJobId(),
                        task.getParcelId(),
                        task.getParcelContentId(),
                        hasSameStatuses,
                        hasSameTile);
                  })
              .toList();
      return duplicatedTilingTasks.stream()
          .map(
              tilingTask -> {
                var parcel = tilingTask.getParcel();
                if (parcel != null) {
                  var parcelContent = parcel.getParcelContent();
                  parcelContent.setTilingStatus(tilingTask.getStatus());
                  return parcel;
                }
                return null;
              })
          .toList();
    }

    var zoneDetectionJob = detectionJobRepository.findById(jobId);
    if (zoneDetectionJob.isPresent()) {
      return parcelDetectionTaskRepository.findAllByJobId(jobId).stream()
          .map(
              detectionTask -> {
                var parcel = detectionTask.getParcel();
                if (parcel != null) {
                  var parcelContent = parcel.getParcelContent();
                  parcelContent.setDetectionStatus(detectionTask.getStatus());
                  return parcel;
                }
                return null;
              })
          .toList();
    }

    throw new NotFoundException("jobId=" + jobId);
  }
}
