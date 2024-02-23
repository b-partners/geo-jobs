package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ParcelService {

  private final TilingTaskRepository tilingTaskRepository;
  private final DetectionTaskRepository detectionTaskRepository;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobRepository detectionJobRepository;

  @Transactional
  public List<Parcel> getParcelsByJobId(String jobId) {
    // TODO: refactor duplicated computing
    var zoneTilingJob = tilingJobRepository.findById(jobId);
    if (zoneTilingJob.isPresent()) {
      return tilingTaskRepository.findAllByJobId(jobId).stream()
          .map(
              tilingTask -> {
                var parcel = tilingTask.getParcel();
                var parcelContent = parcel.getParcelContent();
                parcelContent.setTilingStatus(tilingTask.getStatus());
                return parcel;
              })
          .toList();
    }

    var zoneDetectionJob = detectionJobRepository.findById(jobId);
    if (zoneDetectionJob.isPresent()) {
      return detectionTaskRepository.findAllByJobId(jobId).stream()
          .map(
              detectionTask -> {
                var parcel = detectionTask.getParcel();
                var parcelContent = parcel.getParcelContent();
                parcelContent.setDetectionStatus(detectionTask.getStatus());
                return parcel;
              })
          .toList();
    }

    throw new NotFoundException("jobId=" + jobId);
  }
}
