package app.bpartners.geojobs.job.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.JobAnnotationProcessed;
import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.endpoint.rest.model.JobType;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobAnnotationService {
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final EventProducer eventProducer;

  public AnnotationJobProcessing processAnnotationJob(String jobId) {
    if (tilingJobRepository.findById(jobId).isPresent()) {
      throw new NotImplementedException("Only DETECTION job is handle for now");
    }
    var zoneDetectionJob =
        zoneDetectionJobRepository
            .findById(jobId)
            .orElseThrow(() -> new NotFoundException("ZoneDetection(id=" + jobId + ")"));

    var annotationJobWithoutObjectsId = randomUUID().toString();
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();

    eventProducer.accept(
        List.of(
            JobAnnotationProcessed.builder()
                .jobId(zoneDetectionJob.getId())
                .annotationJobWithObjectsIdTruePositive(annotationJobWithObjectsIdTruePositive)
                .annotationJobWithObjectsIdFalsePositive(annotationJobWithObjectsIdFalsePositive)
                .annotationJobWithoutObjectsId(annotationJobWithoutObjectsId)
                .build()));

    return new AnnotationJobProcessing()
        .jobId(jobId)
        .annotationWithObjectTruePositive(annotationJobWithObjectsIdTruePositive)
        .annotationWithObjectFalsePositive(annotationJobWithObjectsIdFalsePositive)
        .annotationWithoutObjectJobId(annotationJobWithoutObjectsId)
        .jobType(JobType.DETECTION) // TODO: only DETECTION is handle but must be computed
        .creationDatetime(Instant.now());
  }
}
