package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob.ZoomLevelEnum.TOWN;
import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneTilingJobControllerIT extends FacadeIT {
    @Autowired
    ZoneTilingController controller;
    @Autowired
    TilingTaskRepository taskRepository;
    @MockBean
    EventProducer eventProducer;
    @Autowired
    ObjectMapper om;

    CreateZoneTilingJob creatableJob() throws JsonProcessingException {
        return new CreateZoneTilingJob()
                .emailReceiver("mock@hotmail.com")
                .zoneName("Lyon")
                .geoServerUrl("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
                .zoomLevel(TOWN)
                .geoServerParameter(
                        om.readValue(
                                """
                                        {
                                            "service": "WMS",
                                            "request": "GetMap",
                                            "layers": "grandlyon:ortho_2018",
                                            "styles": "",
                                            "format": "image/png",
                                            "transparent": true,
                                            "version": "1.3.0",
                                            "width": 256,
                                            "height": 256,
                                            "srs": "EPSG:3857"
                                          }""",
                                GeoServerParameter.class))
                .features(
                        List.of(
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
                                        .id("feature_1_id")));
    }

    @Test
    void create_tiling_job_ok() throws IOException {
        var created = controller.tileZone(creatableJob());
        var createdList = controller.getTilingJobs(new PageFromOne(1), new BoundedPageSize(30));

        assertNotNull(created.getId());
        assertTrue(createdList.stream().anyMatch(z -> z.equals(created)));
        verify(eventProducer, only()).accept(any());
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

    @Autowired
    ZoneTilingJobRepository tilingJobRepository;

    @Test
    void read_parcel_with_non_emptyTiles() {
        var jobId1 = "job1";
        var jobId2 = "job2";
        var job1 = aZTJ(jobId1);
        var job2 = aZTJ(jobId2);
        var task1 = aTask(jobId1, "task1", "tile1","parcel1");
        var task2 = aTask(jobId2, "task2", "tile2","parcel2");
        tilingJobRepository.saveAll(List.of(job1, job2));
        taskRepository.saveAll(List.of(task1, task2));

        var parcels1 = controller.getZTJParcels(jobId1);
        var parcels2 = controller.getZTJParcels(jobId2);

        assertEquals(1, parcels1.size());
        assertEquals(1, parcels1.get(0).getTiles().size());
        assertEquals("tile1", parcels1.get(0).getTiles().get(0).getId());
        assertEquals(1, parcels2.size());
        assertEquals(1, parcels2.get(0).getTiles().size());
        assertEquals("tile2", parcels2.get(0).getTiles().get(0).getId());
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
                .parcels(List.of(
                        Parcel.builder()
                                .id(parcelId)
                                .parcelContent(
                                        ParcelContent.builder()
                                                .tiles(List.of(Tile.builder().id(tileId).creationDatetime(now).build()))
                                                .creationDatetime(now)
                                                .build()).build()))
                .build();
    }
}
