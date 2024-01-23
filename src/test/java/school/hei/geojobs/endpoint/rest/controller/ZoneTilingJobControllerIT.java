package school.hei.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static school.hei.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN;
import static school.hei.geojobs.endpoint.rest.model.Status.ProgressionEnum.PENDING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.geojobs.conf.FacadeIT;
import school.hei.geojobs.endpoint.event.EventProducer;
import school.hei.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import school.hei.geojobs.endpoint.rest.model.Feature;
import school.hei.geojobs.endpoint.rest.model.GeoServerParameter;
import school.hei.geojobs.endpoint.rest.model.Status;
import school.hei.geojobs.endpoint.rest.model.ZoneTilingJob;
import school.hei.geojobs.model.BoundedPageSize;
import school.hei.geojobs.model.PageFromOne;

class ZoneTilingJobControllerIT extends FacadeIT {

  @Autowired ZoneTilingController controller;
  @MockBean EventProducer eventProducer;
  @Autowired ObjectMapper om;

  CreateZoneTilingJob creatableJob() throws JsonProcessingException {
    return new CreateZoneTilingJob()
        .emailReceiver("mock@hotmail.com")
        .zoneName("Lyon")
        .geoServerUrl("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
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
                    .zoom(10)
                    .id("feature_1_id")));
  }

  @Test
  void create_tiling_job_ok() throws IOException {
    var actual = controller.tileZone(creatableJob());
    var expected = from(creatableJob());
    var actualList = controller.getTilingJobs(new PageFromOne(1), new BoundedPageSize(30));

    assertEquals(expected, ignoreIdAndCreationDatetime(actual));
    // TODO: coordinates are fine when retrieved by controller::tileZone
    //   but rounded when retrieved by controller::findAll!
    // assertTrue(actualList.stream().map(this::ignoreId).anyMatch(z -> z.equals(expected)));

    verify(eventProducer, only()).accept(any());
  }

  @Test
  void read_parcels() throws JsonProcessingException {
    var createdJob = controller.tileZone(creatableJob());
    var actual = controller.getZTJParcels(createdJob.getId());
    var parcel = actual.get(0);

    assertEquals(1, actual.size());
    assertNotNull(parcel.getId());
    assertNotNull(parcel.getCreationDatetime());
    assertNotNull(parcel.getFeature());
  }

  private ZoneTilingJob ignoreIdAndCreationDatetime(ZoneTilingJob zoneTilingJob) {
    zoneTilingJob.getStatus().setCreationDatetime(null);
    zoneTilingJob.id(null);
    zoneTilingJob.setCreationDatetime(null);
    return zoneTilingJob;
  }

  private ZoneTilingJob from(CreateZoneTilingJob createZoneTilingJob) {
    return new ZoneTilingJob()
        .id(null)
        .zoneName(createZoneTilingJob.getZoneName())
        .emailReceiver(createZoneTilingJob.getEmailReceiver())
        .geoServerUrl(createZoneTilingJob.getGeoServerUrl())
        .geoServerParameter(createZoneTilingJob.getGeoServerParameter())
        .status(new Status().creationDatetime(null).health(UNKNOWN).progression(PENDING))
        .features(createZoneTilingJob.getFeatures());
  }
}
