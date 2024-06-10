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
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.TaskStatisticRecomputingSubmittedService;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

public class TaskStatisticRecomputingSubmittedServiceTest {
  private static final String JOB_ID = "jobId";
  MockedConstruction<TaskStatisticMailer> taskMailerMockedConstruction;
  TilingTaskRepository tilingTaskRepositoryMock = mock();
  DetectionTaskRepository detectionTaskRepositoryMock = mock();
  ZoneTilingJobRepository tilingJobRepositoryMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();

  @BeforeEach
  void setUp() {
    taskMailerMockedConstruction = mockConstruction(TaskStatisticMailer.class);
  }

  @AfterEach
  void tearDown() {
    if (taskMailerMockedConstruction != null && !taskMailerMockedConstruction.isClosed())
      taskMailerMockedConstruction.close();
  }

  @Test
  void accept_detection_job_id_ok() {
    TaskStatisticRecomputingSubmittedService subject =
        new TaskStatisticRecomputingSubmittedService(
            tilingTaskRepositoryMock,
            detectionTaskRepositoryMock,
            tilingJobRepositoryMock,
            zoneDetectionJobRepositoryMock,
            mailerMock,
            htmlTemplateParserMock);
    TaskStatisticMailer<ZoneDetectionJob> taskStatisticMailerMock =
        taskMailerMockedConstruction.constructed().getFirst();
    ZoneDetectionJob expectedDetectionJob = ZoneDetectionJob.builder().id(JOB_ID).build();
    when(zoneDetectionJobRepositoryMock.findById(JOB_ID))
        .thenReturn(Optional.of(expectedDetectionJob));
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

  @Test
  void accept_tiling_job_id_ok() {
    TaskStatisticRecomputingSubmittedService subject =
        new TaskStatisticRecomputingSubmittedService(
            tilingTaskRepositoryMock,
            detectionTaskRepositoryMock,
            tilingJobRepositoryMock,
            zoneDetectionJobRepositoryMock,
            mailerMock,
            htmlTemplateParserMock);
    TaskStatisticMailer<ZoneTilingJob> taskStatisticMailerMock =
        taskMailerMockedConstruction.constructed().getLast();
    ZoneTilingJob expectedJob = ZoneTilingJob.builder().id(JOB_ID).build();
    when(tilingJobRepositoryMock.findById(JOB_ID)).thenReturn(Optional.of(expectedJob));
    when(tilingTaskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(
            List.of(
                ZoneTilingJobServiceTest.taskWithStatus(FINISHED, SUCCEEDED),
                ZoneTilingJobServiceTest.taskWithStatus(FINISHED, SUCCEEDED),
                ZoneTilingJobServiceTest.taskWithStatus(PENDING, UNKNOWN),
                ZoneTilingJobServiceTest.taskWithStatus(PENDING, UNKNOWN),
                ZoneTilingJobServiceTest.taskWithStatus(FINISHED, FAILED),
                ZoneTilingJobServiceTest.taskWithStatus(PROCESSING, UNKNOWN)));

    subject.accept(new TaskStatisticRecomputingSubmitted(JOB_ID));

    var taskStatisticCaptor = ArgumentCaptor.forClass(TaskStatistic.class);
    var zoneTilingJobCaptor = ArgumentCaptor.forClass(ZoneTilingJob.class);
    verify(taskStatisticMailerMock, times(1))
        .accept(taskStatisticCaptor.capture(), zoneTilingJobCaptor.capture());
    var taskStatistic = taskStatisticCaptor.getValue();
    var zoneDetectionJob = zoneTilingJobCaptor.getValue();
    var statisticResult = getResult(taskStatistic);
    assertEquals(2, statisticResult.unknownPendingTask().getCount());
    assertEquals(1, statisticResult.unknownProcessingTask().getCount());
    assertEquals(1, statisticResult.failedFinishedTask().getCount());
    assertEquals(2, statisticResult.succeededFinishedTask().getCount());
    assertEquals(0, statisticResult.unknownFinishedTask().getCount());
    assertEquals(expectedJob, zoneDetectionJob);
  }
}
