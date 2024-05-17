package app.bpartners.geojobs.job.service;

import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.endpoint.rest.model.JobType;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.service.event.ZoneDetectionJobAnnotationProcessor;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobAnnotationService {
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobAnnotationProcessor detectionJobAnnotationProcessor;

  public AnnotationJobProcessing processAnnotationJob(String jobId) {
    if (tilingJobRepository.findById(jobId).isPresent()) {
      throw new NotImplementedException("Only DETECTION job is handle for now");
    }
    var zoneDetectionJob =
        zoneDetectionJobRepository
            .findById(jobId)
            .orElseThrow(() -> new NotFoundException("ZoneDetection(id=" + jobId + ")"));

    var annotationJobIds = detectionJobAnnotationProcessor.accept(zoneDetectionJob.getId());

    return new AnnotationJobProcessing()
        .jobId(jobId)
        .annotationWithObjectJobId(annotationJobIds.getJobWithDetectedObjectsId())
        .annotationWithoutObjectJobId(annotationJobIds.getJobWithoutDetectedObjectsId())
        .jobType(JobType.DETECTION) // TODO: only DETECTION is handle but must be computed
        .creationDatetime(Instant.now());
  }
}
