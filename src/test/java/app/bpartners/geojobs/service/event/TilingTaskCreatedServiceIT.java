package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED;
import static app.bpartners.geojobs.file.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.geo.JobType.TILING;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.TilingTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.endpoint.rest.controller.ZoneTilingController;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.model.Tile;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.file.FileHash;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.geo.tiling.TilesDownloader;
import app.bpartners.geojobs.service.geo.tiling.TilingTaskStatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class TilingTaskCreatedServiceIT extends FacadeIT {
  @Autowired TilingTaskCreatedService subject;
  @Autowired ZoneTilingController zoneTilingController;
  @MockBean BucketComponent bucketComponent;
  @MockBean TilesDownloader tilesDownloader;
  @Autowired TilingTaskRepository repository;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @MockBean EventProducer eventProducer;
  @Autowired ObjectMapper om;
  @Autowired TilingTaskStatusService tilingTaskStatusService;

  private Feature lyonFeature;

  @BeforeEach
  void setUp() throws JsonProcessingException {
    when(tilesDownloader.apply(any()))
        .thenAnswer(
            i ->
                Paths.get(this.getClass().getClassLoader().getResource("mockData/lyon").toURI())
                    .toFile());
    when(bucketComponent.upload(any(), any())).thenReturn(new FileHash(SHA256, "mock"));

    lyonFeature =
        om.readValue(
                """
                { "type": "Feature",
                  "properties": {
                    "code": "69",
                    "nom": "Rhône",
                    "id": 30251921,
                    "CLUSTER_ID": 99520,
                    "CLUSTER_SIZE": 386884 },
                  "geometry": {
                    "type": "MultiPolygon",
                    "coordinates": [ [ [
                        [ 4.459648282829194, 45.904988912620688 ],
                        [ 4.464709510872551, 45.928950368349426 ],
                        [ 4.490816965688656, 45.941784543770964 ],
                        [ 4.510354299995861, 45.933697132664598 ],
                        [ 4.518386257467152, 45.912888345521047 ],
                        [ 4.496344031095243, 45.883438201401809 ],
                        [ 4.479593950305621, 45.882900828315755 ],
                        [ 4.459648282829194, 45.904988912620688 ] ] ] ] } }""",
                Feature.class)
            .zoom(10)
            .id("feature_1_id");
  }

  @SneakyThrows
  private app.bpartners.geojobs.endpoint.rest.model.Parcel parcel1() {
    return new app.bpartners.geojobs.endpoint.rest.model.Parcel()
        .creationDatetime(null)
        .id(null)
        .tiles(List.of(tile1(), tile2()))
        .feature(lyonFeature)
        .tilingStatus(
            new Status()
                .creationDatetime(null)
                .progression(Status.ProgressionEnum.FINISHED)
                .health(SUCCEEDED));
  }

  public Tile tile1() {
    return new Tile()
        .creationDatetime(null)
        .id(null)
        .coordinates(new TileCoordinates().x(123132).y(456).z(20))
        .bucketPath("dummy-bucket/lyon/20/123132/456.png");
  }

  public Tile tile2() {
    return new Tile()
        .creationDatetime(null)
        .id(null)
        .coordinates(new TileCoordinates().x(123132).y(123).z(20))
        .bucketPath("dummy-bucket/lyon/20/123132/123.png");
  }

  private app.bpartners.geojobs.endpoint.rest.model.Parcel ignoreIds(
      app.bpartners.geojobs.endpoint.rest.model.Parcel parcel) {
    Status status = parcel.getTilingStatus();
    parcel.setId(null);
    parcel.setCreationDatetime(null);
    assert status != null;
    parcel.setTilingStatus(
        new Status()
            .creationDatetime(null)
            .health(status.getHealth())
            .progression(status.getProgression()));
    return parcel;
  }

  private List<Tile> ignoreIds(List<Tile> tile) {
    for (Tile tile1 : tile) {
      tile1.setId(null);
      tile1.setCreationDatetime(null);
    }
    return tile;
  }

  private ZoneTilingJob aZTJ(String jobId) {
    return ZoneTilingJob.builder()
        .id(jobId)
        .statusHistory(
            (List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .jobType(TILING)
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .build())))
        .zoneName("mock")
        .emailReceiver("mock@hotmail.com")
        .build();
  }

  @SneakyThrows
  private TilingTask aZTT(String jobId, String taskId) {
    return TilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcel(
            Parcel.builder()
                .id(randomUUID().toString())
                .creationDatetime(String.valueOf(Instant.now()))
                .geoServerParameter(new GeoServerParameter().layers("grand-lyon"))
                .feature(lyonFeature)
                .build())
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(taskId)
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .build()))
        .build();
  }

  @SneakyThrows
  private TilingTask aZTT_processing(String jobId, String taskId) {
    return TilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcel(
            Parcel.builder()
                .id(randomUUID().toString())
                .geoServerParameter(new GeoServerParameter().layers("grand-lyon"))
                .feature(
                    om.readValue(
                            """
                                { "type": "Feature",
                                  "properties": {
                                    "code": "69",
                                    "nom": "Rhône",
                                    "id": 30251921,
                                    "CLUSTER_ID": 99520,
                                    "CLUSTER_SIZE": 386884 },
                                  "geometry": {
                                    "type": "MultiPolygon",
                                    "coordinates": [ [ [
                                      [ 4.459648282829194, 45.904988912620688 ],
                                      [ 4.464709510872551, 45.928950368349426 ],
                                      [ 4.490816965688656, 45.941784543770964 ],
                                      [ 4.510354299995861, 45.933697132664598 ],
                                      [ 4.518386257467152, 45.912888345521047 ],
                                      [ 4.496344031095243, 45.883438201401809 ],
                                      [ 4.479593950305621, 45.882900828315755 ],
                                      [ 4.459648282829194, 45.904988912620688 ] ] ] ] } }""",
                            Feature.class)
                        .zoom(10)
                        .id("feature_1_id"))
                .build())
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(taskId)
                    .progression(PROCESSING)
                    .health(UNKNOWN)
                    .build()))
        .build();
  }

  @Test
  void unzip_and_upload_ok() {
    String jobId = randomUUID().toString();
    ZoneTilingJob job =
        zoneTilingJobRepository.save(
            ZoneTilingJob.builder()
                .id(jobId)
                .statusHistory(
                    (List.of(
                        JobStatus.builder()
                            .id(randomUUID().toString())
                            .jobId(jobId)
                            .progression(PENDING)
                            .jobType(TILING)
                            .health(UNKNOWN)
                            .build())))
                .zoneName("mock")
                .emailReceiver("mock@hotmail.com")
                .build());
    String taskId = randomUUID().toString();
    TilingTask toCreate =
        TilingTask.builder()
            .id(taskId)
            .jobId(job.getId())
            .parcel(
                Parcel.builder()
                    .geoServerParameter(new GeoServerParameter().layers("grand-lyon"))
                    .id(randomUUID().toString())
                    .build())
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(taskId)
                        .progression(PENDING)
                        .health(UNKNOWN)
                        .build()))
            .build();
    TilingTask created = repository.save(toCreate);
    TilingTaskCreated createdEventPayload = TilingTaskCreated.builder().task(created).build();

    subject.accept(createdEventPayload);

    int numberOfDirectoryToUpload = 1;
    verify(bucketComponent, times(numberOfDirectoryToUpload)).upload(any(), any(String.class));
  }

  @Test
  void send_statusChanged_event_on_each_status_change() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    TilingTask toCreate = aZTT(jobId, taskId);
    TilingTask created = repository.save(toCreate);
    TilingTaskCreated createdEventPayload = TilingTaskCreated.builder().task(created).build();

    subject.accept(createdEventPayload);

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(2)).accept(eventsCaptor.capture());
    var sentEvents = eventsCaptor.getAllValues().stream().flatMap(List::stream).toList();
    assertEquals(2, sentEvents.size());
    var changedToProcessing = (ZoneTilingJobStatusChanged) sentEvents.get(0);
    assertEquals(PROCESSING, changedToProcessing.getNewJob().getStatus().getProgression());
    var changedToFinished = (ZoneTilingJobStatusChanged) sentEvents.get(1);
    assertEquals(FINISHED, changedToFinished.getNewJob().getStatus().getProgression());
  }

  @Test
  void task_finished_to_processing_ko() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    TilingTask toCreate = aZTT(jobId, taskId);
    TilingTask created = repository.save(toCreate);
    TilingTaskCreated createdEventPayload = TilingTaskCreated.builder().task(created).build();

    subject.accept(createdEventPayload);
    toCreate.setSubmissionInstant(Instant.now());
    TilingTask task1 = repository.findById(taskId).get();
    var sortedStatuses =
        task1.getStatusHistory().stream()
            .sorted(
                comparing(
                    app.bpartners.geojobs.repository.model.Status::getCreationDatetime,
                    naturalOrder()))
            .toList();
    assertThrows(IllegalArgumentException.class, () -> subject.accept(createdEventPayload));
    TilingTask task2 = repository.findById(taskId).get();
    var expectedStatuses =
        task2.getStatusHistory().stream()
            .sorted(
                comparing(
                    app.bpartners.geojobs.repository.model.Status::getCreationDatetime,
                    naturalOrder()))
            .toList();

    assertEquals(sortedStatuses, expectedStatuses);
  }

  @Test
  void task_processing_to_pending_ko() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    TilingTask toCreate = aZTT_processing(jobId, taskId);
    TilingTask created = repository.save(toCreate);
    List<TaskStatus> statuses =
        repository.findById(created.getId()).orElseThrow().getStatusHistory().stream().toList();

    List<TaskStatus> statusesAfterFailedStatusTransition =
        repository.findById(created.getId()).orElseThrow().getStatusHistory().stream().toList();

    assertEquals(statusesAfterFailedStatusTransition, statuses);
    assertFalse(statuses.isEmpty());
    assertFalse(statusesAfterFailedStatusTransition.isEmpty());
  }

  @Test
  void get_ztj_tiles() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    TilingTask toCreate = aZTT(jobId, randomUUID().toString());
    TilingTask created = repository.save(toCreate);
    TilingTaskCreated ztjCreated = TilingTaskCreated.builder().task(created).build();

    subject.accept(ztjCreated);
    List<app.bpartners.geojobs.endpoint.rest.model.Parcel> actual =
        zoneTilingController.getZTJParcels(jobId);

    actual.forEach(this::ignoreIds);
    actual.forEach(parcel -> ignoreIds(parcel.getTiles()));
    assertEquals(List.of(parcel1()), actual);
  }
}
