package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.annotation.JobAnnotationProcessed;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobAnnotationProcessedService implements Consumer<JobAnnotationProcessed> {
  private final ZoneDetectionJobAnnotationProcessor zoneDetectionJobAnnotationProcessor;

  @Override
  public void accept(JobAnnotationProcessed jobAnnotationProcessed) {
    var jobId = jobAnnotationProcessed.getJobId();
    var annotationJobWithObjectsIdTruePositive =
        jobAnnotationProcessed.getAnnotationJobWithObjectsIdTruePositive();
    var annotationJobWithObjectsIdFalsePositive =
        jobAnnotationProcessed.getAnnotationJobWithObjectsIdFalsePositive();
    var annotationJobWithoutObjectsId = jobAnnotationProcessed.getAnnotationJobWithoutObjectsId();
    var minConfidence = jobAnnotationProcessed.getMinConfidence();

    zoneDetectionJobAnnotationProcessor.accept(
        jobId,
        minConfidence,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId);
  }
}
