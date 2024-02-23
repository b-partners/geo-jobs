package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
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
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobRepository detectionJobRepository;

  @Transactional
  public List<Parcel> getParcelsByJobId(String jobId) {
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
      throw new NotImplementedException("TODO");
    }

    throw new NotFoundException("jobId=" + jobId);
  }
}
