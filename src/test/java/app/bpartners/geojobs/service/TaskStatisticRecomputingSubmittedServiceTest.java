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
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.TaskStatisticRecomputingSubmittedService;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.ArrayList;
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
  ParcelDetectionTaskRepository parcelDetectionTaskRepositoryMock = mock();
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
            parcelDetectionTaskRepositoryMock,
            tilingJobRepositoryMock,
            zoneDetectionJobRepositoryMock,
            mailerMock,
            htmlTemplateParserMock);
    TaskStatisticMailer<ZoneDetectionJob> taskStatisticMailerMock =
        taskMailerMockedConstruction.constructed().getFirst();
    ZoneDetectionJob expectedDetectionJob = ZoneDetectionJob.builder().id(JOB_ID).build();
    when(zoneDetectionJobRepositoryMock.findById(JOB_ID))
        .thenReturn(Optional.of(expectedDetectionJob));
    when(parcelDetectionTaskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(
            List.of(
                taskWithStatus(FINISHED, SUCCEEDED, someParcels(0)),
                taskWithStatus(FINISHED, SUCCEEDED, someParcels(0)),
                taskWithStatus(PENDING, UNKNOWN, someParcels(0)),
                taskWithStatus(PENDING, UNKNOWN, someParcels(0)),
                taskWithStatus(FINISHED, FAILED),
                taskWithStatus(PROCESSING, UNKNOWN, someParcels(0))));

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
    assertEquals(0, taskStatistic.getTilesCount());
    assertEquals(expectedDetectionJob, zoneDetectionJob);
  }

  private Parcel someParcels(Integer parcelNb) {
    List<Tile> tiles = new ArrayList<>();
    for (int i = 0; i < parcelNb; i++) {
      tiles.add(new Tile());
    }
    return Parcel.builder().parcelContent(ParcelContent.builder().tiles(tiles).build()).build();
  }

  @Test
  void accept_tiling_job_id_ok() {
    TaskStatisticRecomputingSubmittedService subject =
        new TaskStatisticRecomputingSubmittedService(
            tilingTaskRepositoryMock,
            parcelDetectionTaskRepositoryMock,
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
                ZoneTilingJobServiceTest.taskWithStatus(FINISHED, SUCCEEDED, someParcels(10)),
                ZoneTilingJobServiceTest.taskWithStatus(FINISHED, SUCCEEDED, someParcels(8)),
                ZoneTilingJobServiceTest.taskWithStatus(PENDING, UNKNOWN),
                ZoneTilingJobServiceTest.taskWithStatus(PENDING, UNKNOWN),
                ZoneTilingJobServiceTest.taskWithStatus(FINISHED, FAILED, someParcels(0)),
                ZoneTilingJobServiceTest.taskWithStatus(PROCESSING, UNKNOWN, someParcels(0))));

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
    assertEquals(18, taskStatistic.getTilesCount());
    assertEquals(expectedJob, zoneDetectionJob);
  }
}
