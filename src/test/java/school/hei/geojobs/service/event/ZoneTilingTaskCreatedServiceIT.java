package school.hei.geojobs.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.geojobs.endpoint.event.gen.ZoneTilingTaskCreated;
import school.hei.geojobs.file.FileHash;
import school.hei.geojobs.file.FileHashAlgorithm;
import school.hei.geojobs.repository.model.TilingJobStatus;
import school.hei.geojobs.repository.model.TilingTaskStatus;
import school.hei.geojobs.service.geo.TilesDownloader;
import school.hei.geojobs.conf.FacadeIT;
import school.hei.geojobs.endpoint.event.EventProducer;
import school.hei.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import school.hei.geojobs.endpoint.rest.controller.ZoneTilingController;
import school.hei.geojobs.endpoint.rest.model.Feature;
import school.hei.geojobs.endpoint.rest.model.GeoServerParameter;
import school.hei.geojobs.endpoint.rest.model.Status;
import school.hei.geojobs.endpoint.rest.model.Tile;
import school.hei.geojobs.endpoint.rest.model.TileCoordinates;
import school.hei.geojobs.file.BucketComponent;
import school.hei.geojobs.repository.ZoneTilingJobRepository;
import school.hei.geojobs.repository.ZoneTilingTaskRepository;
import school.hei.geojobs.repository.model.ZoneTilingJob;
import school.hei.geojobs.repository.model.ZoneTilingTask;
import school.hei.geojobs.repository.model.geo.Parcel;

import java.nio.file.Paths;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static school.hei.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED;
import static school.hei.geojobs.endpoint.rest.model.Status.ProgressionEnum.FINISHED;
import static school.hei.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;

@Slf4j
class ZoneTilingTaskCreatedServiceIT extends FacadeIT {
  @Autowired ZoneTilingTaskCreatedService subject;
  @Autowired
  ZoneTilingController zoneTilingController;
  @MockBean BucketComponent bucketComponent;
  @MockBean
  TilesDownloader tilesDownloader;
  @Autowired ZoneTilingTaskRepository repository;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @MockBean EventProducer eventProducer;
  @Autowired ObjectMapper om;

  @BeforeEach
  void setUp() {
    when(tilesDownloader.apply(any()))
        .thenAnswer(
            i -> Paths.get(this.getClass().getClassLoader().getResource("mockData/lyon").toURI()).toFile()
            );
    when(bucketComponent.upload(any(), any())).thenReturn(new FileHash(FileHashAlgorithm.SHA256, "mock"));
  }

  @SneakyThrows
  private school.hei.geojobs.endpoint.rest.model.Parcel parcel1(){
    return new school.hei.geojobs.endpoint.rest.model.Parcel()
        .creationDatetime(null)
        .id(null)
        .tiles(List.of(tile1(), tile2()))
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
                  "coordinates": [[[[4.459648282829194, 45.90498891262069], [4.464709510872551, 45.928950368349426], [4.490816965688656, 45.941784543770964], [4.510354299995861, 45.9336971326646], [4.518386257467152, 45.91288834552105], [4.496344031095243, 45.88343820140181], [4.479593950305621, 45.882900828315755], [4.459648282829194, 45.90498891262069]]]] } }""",
                    Feature.class)
                .zoom(10)
                .id("feature_1_id"))
        .tilingStatus(new Status().creationDatetime(null)
            .progression(FINISHED)
            .health(SUCCEEDED));
  }

  public Tile tile1 (){
    return new Tile()
        .creationDatetime(null)
        .id(null)
        .coordinates(new TileCoordinates()
            .x(123132)
            .y(456)
            .z(20))
        .bucketPath(null);
  }

  public Tile tile2 (){
    return new Tile()
        .creationDatetime(null)
        .id(null)
        .coordinates(new TileCoordinates()
            .x(123132)
            .y(123)
            .z(20))
        .bucketPath(null);
  }

  private school.hei.geojobs.endpoint.rest.model.Parcel ignoreIds(school.hei.geojobs.endpoint.rest.model.Parcel parcel){
    Status status = parcel.getTilingStatus();
    parcel.setId(null);
    assert status != null;
    parcel.setTilingStatus(new Status()
        .creationDatetime(null)
        .health(status.getHealth())
        .progression(status.getProgression())
    );
    return parcel;
  }

  private List<Tile> ignoreIds(List<Tile> tile){
    for (Tile tile1 : tile) {
      tile1.setId(null);
      tile1.setCreationDatetime(null);
      tile1.setBucketPath(null);
    }
    return tile;
  }

  private ZoneTilingJob aZTJ(String jobId){
    return ZoneTilingJob.builder()
        .id(jobId)
        .statusHistory(
            (List.of(
                TilingJobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .build())))
        .zoneName("mock")
        .emailReceiver("mock@hotmail.com")
        .build();
  }

  @SneakyThrows
  private ZoneTilingTask aZTT(String jobId, String taskId){
    return  ZoneTilingTask.builder()
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
                TilingTaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(taskId)
                    .progression(PENDING)
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
                        TilingJobStatus.builder()
                            .id(randomUUID().toString())
                            .jobId(jobId)
                            .progression(PENDING)
                            .health(UNKNOWN)
                            .build())))
                .zoneName("mock")
                .emailReceiver("mock@hotmail.com")
                .build());
    String taskId = randomUUID().toString();
    ZoneTilingTask toCreate =
        ZoneTilingTask.builder()
            .id(taskId)
            .jobId(job.getId())
            .parcel(
                Parcel.builder()
                    .geoServerParameter(new GeoServerParameter().layers("grand-lyon"))
                    .id(randomUUID().toString())
                    .build())
            .statusHistory(
                List.of(
                    TilingTaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(taskId)
                        .progression(PENDING)
                        .health(UNKNOWN)
                        .build()))
            .build();
    ZoneTilingTask created = repository.save(toCreate);
    ZoneTilingTaskCreated createdEventPayload =
        ZoneTilingTaskCreated.builder().task(created).build();

    subject.accept(createdEventPayload);

    int numberOfDirectoryToUpload = 1;
    verify(bucketComponent, times(numberOfDirectoryToUpload)).upload(any(), any(String.class));
  }

  @Test
  void send_statusChanged_event_on_each_status_change() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    ZoneTilingTask toCreate = aZTT(jobId, taskId);
    ZoneTilingTask created = repository.save(toCreate);
    ZoneTilingTaskCreated createdEventPayload =
        ZoneTilingTaskCreated.builder().task(created).build();

    subject.accept(createdEventPayload);

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(2)).accept(eventCaptor.capture());
    var sentEvents = eventCaptor.getAllValues().stream().flatMap(List::stream).toList();
    assertEquals(2, sentEvents.size());
    var changedToProcessing = (ZoneTilingJobStatusChanged) sentEvents.get(0);
    assertEquals(PROCESSING, changedToProcessing.getNewJob().getStatus().getProgression());
    var changedToFinished = (ZoneTilingJobStatusChanged) sentEvents.get(1);
    assertEquals(school.hei.geojobs.repository.model.Status.ProgressionStatus.FINISHED, changedToFinished.getNewJob().getStatus().getProgression());
  }

  @Test
  void task_finished_to_processing_ko(){
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    ZoneTilingTask toCreate = aZTT(jobId, taskId);
    ZoneTilingTask created = repository.save(toCreate);
    ZoneTilingTaskCreated createdEventPayload =
        ZoneTilingTaskCreated.builder().task(created).build();

    subject.accept(createdEventPayload);
    toCreate.setSubmissionInstant(Instant.now());
    ZoneTilingTask taskCreated = repository.save(toCreate);
    ZoneTilingTaskCreated taskEvent =
        ZoneTilingTaskCreated.builder().task(taskCreated).build();

    assertThrows(IllegalArgumentException.class , () -> subject.accept(taskEvent));
  }

  @Test
  void get_ztj_tiles() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    ZoneTilingTask toCreate = aZTT(jobId, randomUUID().toString());
    ZoneTilingTask created = repository.save(toCreate);
    ZoneTilingTaskCreated ztjCreated =
        ZoneTilingTaskCreated.builder().task(created).build();

    subject.accept(ztjCreated);
    List<school.hei.geojobs.endpoint.rest.model.Parcel> actual = zoneTilingController.getZTJParcels(jobId);

    actual.forEach(this::ignoreIds);
    actual.forEach(parcel -> ignoreIds(parcel.getTiles()));
    assertEquals(List.of(parcel1()), actual);
  }
}
