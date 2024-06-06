package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.service.ZoneDetectionJobServiceTest.taskWithStatus;
import static app.bpartners.geojobs.service.ZoneTilingJobServiceTest.getResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.TaskStatisticRecomputingSubmittedService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TaskStatisticRecomputingSubmittedServiceTest {
  private static final String JOB_ID = "jobId";
  ZoneDetectionJobService detectionJobServiceMock = mock();
  DetectionTaskRepository detectionTaskRepositoryMock = mock();
  ZDJTaskStatisticMailer taskStatisticMailerMock = mock();
  TaskStatisticRecomputingSubmittedService subject =
      new TaskStatisticRecomputingSubmittedService(
          detectionJobServiceMock, detectionTaskRepositoryMock, taskStatisticMailerMock);

  @Test
  void accept_ok() {
    ZoneDetectionJob expectedDetectionJob = ZoneDetectionJob.builder().id(JOB_ID).build();
    when(detectionJobServiceMock.findById(JOB_ID)).thenReturn(expectedDetectionJob);
    when(detectionTaskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(
            List.of(
                taskWithStatus(FINISHED, SUCCEEDED),
                taskWithStatus(FINISHED, SUCCEEDED),
                taskWithStatus(PENDING, UNKNOWN),
                taskWithStatus(PENDING, UNKNOWN),
                taskWithStatus(FINISHED, FAILED),
                taskWithStatus(PROCESSING, UNKNOWN)));

    subject.accept(new TaskStatisticRecomputingSubmitted(JOB_ID));

    var taskStatisticCaptor = ArgumentCaptor.forClass(TaskStatistic.class);
    var zoneDetectionJobCaptor = ArgumentCaptor.forClass(ZoneDetectionJob.class);
    verify(taskStatisticMailerMock, times(1))
        .accept(taskStatisticCaptor.capture(), zoneDetectionJobCaptor.capture());
    var taskStatistic = taskStatisticCaptor.getValue();
    var zoneDetectionJob = zoneDetectionJobCaptor.getValue();
    var statisticResult = getResult(taskStatistic);
    assertEquals(2, statisticResult.unknownPendingTask().getCount());
    assertEquals(1, statisticResult.unknownProcessingTask().getCount());
    assertEquals(1, statisticResult.failedFinishedTask().getCount());
    assertEquals(2, statisticResult.succeededFinishedTask().getCount());
    assertEquals(0, statisticResult.unknownFinishedTask().getCount());
    assertEquals(expectedDetectionJob, zoneDetectionJob);
  }
}
