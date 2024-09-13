package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.STARTED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.event.annotation.delivery.AnnotationDeliveryJobStatusChangedService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnotationDeliveryJobStatusChangedServiceTest {
  StatusChangedHandler statusChangedHandlerMock = new StatusChangedHandler();
  AnnotationService annotationServiceMock = mock();
  AnnotationDeliveryJobStatusChangedService subject =
      new AnnotationDeliveryJobStatusChangedService(
          statusChangedHandlerMock, annotationServiceMock);

  @Test
  void accept_finished_event_ok() {
    var oldJob = someDeliveryJob(PROCESSING, UNKNOWN);
    var newJob =
        someDeliveryJob(FINISHED, SUCCEEDED).toBuilder()
            .detectionJobId(randomUUID().toString())
            .annotationJobId(randomUUID().toString())
            .annotationJobName("dummyJobName")
            .labels(List.of(new Label().id(randomUUID().toString()).color("#000").name("POOl")))
            .build();

    subject.accept(
        AnnotationDeliveryJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build());

    verify(annotationServiceMock, only())
        .saveAnnotationJob(
            newJob.getDetectionJobId(),
            newJob.getAnnotationJobId(),
            newJob.getAnnotationJobName(),
            newJob.getLabels(),
            STARTED);
  }

  private AnnotationDeliveryJob someDeliveryJob(
      Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return AnnotationDeliveryJob.builder()
        .statusHistory(
            List.of(
                JobStatus.builder().health(healthStatus).progression(progressionStatus).build()))
        .build();
  }
}
