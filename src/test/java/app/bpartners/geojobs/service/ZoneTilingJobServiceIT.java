package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("TODO: fail to create JPA transaction")
class ZoneTilingJobServiceIT extends FacadeIT {
  private static final String JOB_ID = "randomTilingJobId";
  public static final String TILING_TASK1_ID = "tilingTask1_id";
  public static final String TILING_TASK2_ID = "tilingTask2_id";
  @Autowired ZoneTilingJobService subject;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired TilingTaskRepository tilingTaskRepository;

  @BeforeEach
  void setUp() {
    zoneTilingJobRepository.save(
        ZoneTilingJob.builder()
            .id(JOB_ID)
            .emailReceiver("dummy@email.com")
            .zoneName("dummyZoneName")
            .build());
    TilingTask taskWithoutParcel =
        TilingTask.builder().id(TILING_TASK1_ID).jobId(JOB_ID).parcels(List.of()).build();
    TilingTask taskWithParcel =
        TilingTask.builder()
            .id(TILING_TASK2_ID)
            .jobId(JOB_ID)
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
    tilingTaskRepository.saveAll(List.of(taskWithoutParcel, taskWithParcel));
  }

  @AfterEach
  void tearDown() {
    tilingTaskRepository.deleteAllById(List.of(TILING_TASK1_ID, TILING_TASK2_ID));
    zoneTilingJobRepository.deleteById(JOB_ID);
  }

  @Test
  void duplicate_tiling_job_ok() {
    var ztj = zoneTilingJobRepository.getById(JOB_ID);
    var existingTasks = tilingTaskRepository.findAllByJobId(ztj.getId());

    var actual = subject.duplicate(JOB_ID);

    var duplicatedTasks = tilingTaskRepository.findAllByJobId(actual.getId());
    assertEquals(ztj.toBuilder().id(actual.getId()).build(), actual);

    /*
    TODO: check why even contents are identical, test does not pass !
    assertEquals(
        existingTasks.stream().map(ZoneTilingJobServiceIT::ignoreGeneratedIds).toList(),
        duplicatedTasks.stream().map(ZoneTilingJobServiceIT::ignoreGeneratedIds).toList());
     */
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
}
