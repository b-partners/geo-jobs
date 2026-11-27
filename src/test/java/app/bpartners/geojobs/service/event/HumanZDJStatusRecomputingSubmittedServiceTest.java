package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.status.HumanZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HumanZDJStatusRecomputingSubmittedServiceTest {
  private static final String ZDJ_ID = "humanZdjId";

  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  AnnotationRetrievingJobService retrievingJobServiceMock = mock();
  JobStatusRepository jobStatusRepositoryMock = mock();
  GeoJsonConversionJobService geoJsonConversionJobServiceMock = mock();
  EntityManager entityManagerMock = mock();
  HumanZDJStatusRecomputingSubmittedService subject =
      new HumanZDJStatusRecomputingSubmittedService(
          zoneDetectionJobServiceMock,
          retrievingJobServiceMock,
          jobStatusRepositoryMock,
          geoJsonConversionJobServiceMock,
          entityManagerMock);

  private ZoneDetectionJob humanZdj(
      app.bpartners.geojobs.job.model.Status.ProgressionStatus progression,
      app.bpartners.geojobs.job.model.Status.HealthStatus health) {
    return ZoneDetectionJob.builder()
        .id(ZDJ_ID)
        .statusHistory(
            new ArrayList<>(
                List.of(
                    JobStatus.builder()
                        .id("initialStatus")
                        .jobId(ZDJ_ID)
                        .progression(progression)
                        .health(health)
                        .creationDatetime(now().minusSeconds(60))
                        .build())))
        .build();
  }

  private AnnotationRetrievingJob retrievingJob(boolean succeeded) {
    var job = mock(AnnotationRetrievingJob.class);
    when(job.isSucceeded()).thenReturn(succeeded);
    return job;
  }

  private void accept() {
    var event = new HumanZDJStatusRecomputingSubmitted(ZDJ_ID);

    var error = assertThrows(RuntimeException.class, () -> subject.accept(event));
    assertEquals("Fail on purpose so that message is not ack, causing retry", error.getMessage());
  }

  @Test
  void accept_marks_zdj_finished_and_computes_geojson_when_all_retrieving_jobs_succeeded() {
    var zdj = humanZdj(PROCESSING, UNKNOWN);
    var retrievingJobs = List.of(retrievingJob(true), retrievingJob(true));
    when(zoneDetectionJobServiceMock.findById(ZDJ_ID)).thenReturn(zdj);
    when(retrievingJobServiceMock.findAllByDetectionJobId(ZDJ_ID)).thenReturn(retrievingJobs);

    accept();

    var statusCaptor = ArgumentCaptor.forClass(JobStatus.class);
    verify(jobStatusRepositoryMock).save(statusCaptor.capture());
    assertEquals(FINISHED, statusCaptor.getValue().getProgression());
    assertEquals(SUCCEEDED, statusCaptor.getValue().getHealth());
    assertEquals(ZDJ_ID, statusCaptor.getValue().getJobId());
    verify(geoJsonConversionJobServiceMock).getOrComputeGeoJsonUrl(ZDJ_ID);
    verify(entityManagerMock).detach(zdj);
    retrievingJobs.forEach(job -> verify(entityManagerMock).detach(job));
  }

  @Test
  void accept_keeps_zdj_status_when_some_retrieving_job_is_not_succeeded() {
    var zdj = humanZdj(PROCESSING, UNKNOWN);
    var retrievingJobs = List.of(retrievingJob(true), retrievingJob(false));
    when(zoneDetectionJobServiceMock.findById(ZDJ_ID)).thenReturn(zdj);
    when(retrievingJobServiceMock.findAllByDetectionJobId(ZDJ_ID)).thenReturn(retrievingJobs);

    accept();

    verifyNoInteractions(jobStatusRepositoryMock);
    verifyNoInteractions(geoJsonConversionJobServiceMock);
  }

  @Test
  void accept_does_not_recompute_anything_when_zdj_is_already_finished_and_succeeded() {
    var zdj = humanZdj(FINISHED, SUCCEEDED);
    var retrievingJobs = List.of(retrievingJob(true));
    when(zoneDetectionJobServiceMock.findById(ZDJ_ID)).thenReturn(zdj);
    when(retrievingJobServiceMock.findAllByDetectionJobId(ZDJ_ID)).thenReturn(retrievingJobs);

    accept();

    verifyNoInteractions(jobStatusRepositoryMock);
    verifyNoInteractions(geoJsonConversionJobServiceMock);
  }
}
