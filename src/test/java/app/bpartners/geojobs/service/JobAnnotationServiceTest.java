package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.ZoneDetectionJobAnnotationProcessor;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class JobAnnotationServiceTest {
  public static final String TILING_JOB = "tilingJob";
  public static final String DETECTION_JOB_ID = "detectionJobId";
  public static final String JOB_WITH_DETECTED_OBJECTS_ID = "jobWithDetectedObjectsId";
  public static final String JOB_WITHOUT_DETECTED_OBJECTS_ID = "jobWithoutDetectedObjectsId";
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  JobAnnotationService subject =
      new JobAnnotationService(
          zoneDetectionJobRepositoryMock, tilingJobRepositoryMock, jobAnnotationProcessorMock);

  @Test
  void process_annotation_job_ko() {
    when(tilingJobRepositoryMock.findById(TILING_JOB))
        .thenReturn(Optional.of(ZoneTilingJob.builder().build()));
    assertThrows(NotImplementedException.class, () -> subject.processAnnotationJob(TILING_JOB));
  }

  @Test
  void process_annotation_job_ok() {
    when(zoneDetectionJobRepositoryMock.findById(DETECTION_JOB_ID))
        .thenReturn(Optional.of(ZoneDetectionJob.builder().id(DETECTION_JOB_ID).build()));
    when(jobAnnotationProcessorMock.accept(DETECTION_JOB_ID))
        .thenReturn(
            new ZoneDetectionJobAnnotationProcessor.AnnotationJobIds(
                JOB_WITH_DETECTED_OBJECTS_ID, JOB_WITHOUT_DETECTED_OBJECTS_ID));

    AnnotationJobProcessing actual = subject.processAnnotationJob(DETECTION_JOB_ID);

    assertEquals(JOB_WITH_DETECTED_OBJECTS_ID, actual.getAnnotationWithObjectJobId());
    assertEquals(JOB_WITHOUT_DETECTED_OBJECTS_ID, actual.getAnnotationWithoutObjectJobId());
    assertEquals(DETECTION_JOB_ID, actual.getJobId());
  }
}
