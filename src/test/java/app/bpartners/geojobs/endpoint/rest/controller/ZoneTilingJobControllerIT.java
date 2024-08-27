package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob.ZoomLevelEnum.TOWN;
import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.TilingJobDuplicatedMailer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE)
class ZoneTilingJobControllerIT extends FacadeIT {
  private static final String JOB_ID = "randomTilingJobId";
  public static final String TILING_TASK1_ID = "tilingTask1_id";
  public static final String TILING_TASK2_ID = "tilingTask2_id";
  @Autowired ZoneTilingController controller;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired TilingTaskRepository taskRepository;
  @MockBean EventProducer eventProducer;
  @Autowired ObjectMapper om;
  @Autowired ZoneTilingJobMapper tilingJobMapper;
  @Autowired ZoneDetectionJobRepository detectionJobRepository;
  @MockBean TilingJobDuplicatedMailer tilingJobDuplicatedMailerMock;

  @BeforeEach
  void setUp() {
    zoneTilingJobRepository.save(
        ZoneTilingJob.builder()
            .id(JOB_ID)
            .emailReceiver("dummy@email.com")
            .zoneName("dummyZoneName")
            .build());
    TilingTask taskWithoutParcel =
        TilingTask.builder()
            .id(TILING_TASK1_ID)
            .jobId(JOB_ID)
            .parcels(List.of())
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id("taskStatus1_id")
                        .taskId(TILING_TASK2_ID)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .build();
    TilingTask taskWithParcel =
        TilingTask.builder()
            .id(TILING_TASK2_ID)
            .jobId(JOB_ID)
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id("taskStatus1_id")
                        .taskId(TILING_TASK2_ID)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .parcels(
                List.of(
                    Parcel.builder()
                        .id("parcel1_id")
                        .parcelContent(
                            ParcelContent.builder()
                                .id("parcelContent1_id")
                                .tiles(List.of(new Tile()))
                                .build())
                        .build()))
            .build();
    taskRepository.saveAll(List.of(taskWithoutParcel, taskWithParcel));
  }

  @AfterEach
  void tearDown() {
    taskRepository.deleteAllById(List.of(TILING_TASK1_ID, TILING_TASK2_ID));
    zoneTilingJobRepository.deleteById(JOB_ID);
  }

  @Test
  void duplicate_tiling_job_ok() {
    var ztj = zoneTilingJobRepository.getById(JOB_ID);
    var existingTasks = taskRepository.findAllByJobId(JOB_ID);

    var actual = controller.duplicateTilingJob(JOB_ID);

    var restExpectedJob =
        tilingJobMapper.toRest(ztj.toBuilder().id(actual.getId()).build(), List.of(), true);
    var expectedJob =
        restExpectedJob
            .creationDatetime(actual.getCreationDatetime())
            .status(
                restExpectedJob
                    .getStatus()
                    .creationDatetime(actual.getStatus().getCreationDatetime()));
    assertEquals(expectedJob.id(actual.getId()), actual);
    /*TODO: set into ZoneTilingJobWithoutTasksCreated IT
    var duplicatedTasks = taskRepository.findAllByJobId(actual.getId());
    assertEquals(
        existingTasks.stream().map(ZoneTilingJobControllerIT::ignoreGeneratedIds).toList(),
        duplicatedTasks.stream().map(ZoneTilingJobControllerIT::ignoreGeneratedIds).toList());
    var associatedDetectionJobs = detectionJobRepository.findAllByZoneTilingJob_Id(actual.getId());
    assertEquals(2, associatedDetectionJobs.size());
    assertTrue(
        associatedDetectionJobs.stream().anyMatch(job -> job.getDetectionType().equals(HUMAN)));
    assertTrue(
        associatedDetectionJobs.stream().anyMatch(job -> job.getDetectionType().equals(MACHINE)));
    verify(tilingJobDuplicatedMailerMock, times(1)).accept(any());
    taskRepository.deleteAllById(duplicatedTasks.stream().map(TilingTask::getId).toList());
    zoneTilingJobRepository.deleteById(actual.getId());*/
  }

  private static TilingTask ignoreGeneratedIds(TilingTask task) {
    task.setId(null);
    if (!task.getParcels().isEmpty()) {
      var parcel = task.getParcel();
      parcel.setId(null);
      var parcelContent = parcel.getParcelContent();
      parcelContent.setId(null);
      parcelContent.setCreationDatetime(null);
      if (!parcelContent.getTiles().isEmpty()) {
        var tile = parcelContent.getFirstTile();
        tile.setId(null);
        tile.setCreationDatetime(null);
      }
    }
    return task;
  }

  CreateZoneTilingJob creatableJob() throws JsonProcessingException {
    return new CreateZoneTilingJob()
        .emailReceiver("mock@hotmail.com")
        .zoneName("Lyon")
        .geoServerUrl("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
        .zoomLevel(TOWN)
        .geoServerParameter(
            om.readValue(
                "{\n"
                    + "    \"service\": \"WMS\",\n"
                    + "    \"request\": \"GetMap\",\n"
                    + "    \"layers\": \"grandlyon:ortho_2018\",\n"
                    + "    \"styles\": \"\",\n"
                    + "    \"format\": \"image/png\",\n"
                    + "    \"transparent\": true,\n"
                    + "    \"version\": \"1.3.0\",\n"
                    + "    \"width\": 256,\n"
                    + "    \"height\": 256,\n"
                    + "    \"srs\": \"EPSG:3857\"\n"
                    + "  }",
                GeoServerParameter.class))
        .features(
            List.of(
                om.readValue(
                        "{ \"type\": \"Feature\",\n"
                            + "  \"properties\": {\n"
                            + "    \"code\": \"69\",\n"
                            + "    \"nom\": \"Rh\u00f4ne\",\n"
                            + "    \"id\": 30251921,\n"
                            + "    \"CLUSTER_ID\": 99520,\n"
                            + "    \"CLUSTER_SIZE\": 386884 },\n"
                            + "  \"geometry\": {\n"
                            + "    \"type\": \"MultiPolygon\",\n"
                            + "    \"coordinates\": [ [ [\n"
                            + "      [ 4.459648282829194, 45.904988912620688 ],\n"
                            + "      [ 4.464709510872551, 45.928950368349426 ],\n"
                            + "      [ 4.490816965688656, 45.941784543770964 ],\n"
                            + "      [ 4.510354299995861, 45.933697132664598 ],\n"
                            + "      [ 4.518386257467152, 45.912888345521047 ],\n"
                            + "      [ 4.496344031095243, 45.883438201401809 ],\n"
                            + "      [ 4.479593950305621, 45.882900828315755 ],\n"
                            + "      [ 4.459648282829194, 45.904988912620688 ] ] ] ] } }",
                        Feature.class)
                    .id("feature_1_id")));
  }

  @Test
  void create_tiling_job_ok() throws IOException {
    var created = controller.tileZone(creatableJob());

    var createdList = controller.getTilingJobs(new PageFromOne(1), new BoundedPageSize(30));
    assertNotNull(created.getId());
    assertTrue(createdList.stream().anyMatch((z) -> z.equals(created.features(List.of()))));
    verify(eventProducer, times(2)).accept(any());
  }

  @Test
  void read_parcels_right_after_job_creation() throws JsonProcessingException {
    var createdJob = controller.tileZone(creatableJob());
    var parcels = controller.getZTJParcels(createdJob.getId());
    var parcel = parcels.get(0);
    assertEquals(ZoomLevelEnum.TOWN, createdJob.getZoomLevel());
    assertEquals(1, parcels.size());
    assertNotNull(parcel.getId());
    assertNotNull(parcel.getCreationDatetime());
    assertNotNull(parcel.getFeature());
    assertNotNull(parcel.getTiles());
    assertEquals(14, parcel.getFeature().getZoom());
  }

  @Autowired ZoneTilingJobRepository tilingJobRepository;

  @Test
  void read_parcel_with_non_emptyTiles() {
    var jobId1 = "job1";
    var jobId2 = "job2";
    var job1 = aZTJ(jobId1);
    var job2 = aZTJ(jobId2);
    var task1 = aTask(jobId1, "task1", "tile1", "parcel1");
    var task2 = aTask(jobId2, "task2", "tile2", "parcel2");
    tilingJobRepository.saveAll(List.of(job1, job2));
    taskRepository.saveAll(List.of(task1, task2));

    var parcels1 = controller.getZTJParcels(jobId1);
    var parcels2 = controller.getZTJParcels(jobId2);

    assertEquals(1, parcels1.size());
    assertEquals(1, parcels1.get(0).getTiles().size());
    assertNotNull(parcels1.get(0).getTiles().get(0).getId());
    assertEquals(1, parcels2.size());
    assertEquals(1, parcels2.get(0).getTiles().size());
    assertNotNull(parcels2.get(0).getTiles().get(0).getId());
  }

  @NotNull
  private ZoneTilingJob aZTJ(String jobId) {
    var job = new ZoneTilingJob();
    job.setId(jobId);
    job.setEmailReceiver("dummy@email.com");
    job.setZoneName("dummy");
    return job;
  }

  private static TilingTask aTask(String jobId, String taskId, String tileId, String parcelId) {
    var now = now();
    return TilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .submissionInstant(now)
        .parcels(
            List.of(
                Parcel.builder()
                    .id(parcelId)
                    .parcelContent(
                        ParcelContent.builder()
                            .tiles(List.of(Tile.builder().id(tileId).creationDatetime(now).build()))
                            .creationDatetime(now)
                            .build())
                    .build()))
        .build();
  }
}
