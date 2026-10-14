package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FINISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson.CityJSONRequestStatusMapper;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import org.junit.jupiter.api.Test;

class CityJSONRequestStatusMapperTest {
  @Test
  void failed_status_exposes_message() {
    var status = CityJSONRequestStatusMapper.toGenericStatusRest(FAILED, "Invalid CityJSON file");

    assertEquals(Status.HealthEnum.FAILED, status.getHealth());
    assertEquals("Invalid CityJSON file", status.getMessage());
  }

  @Test
  void finished_status_has_no_message() {
    var status = CityJSONRequestStatusMapper.toGenericStatusRest(FINISHED, null);

    assertEquals(Status.HealthEnum.SUCCEEDED, status.getHealth());
    assertNull(status.getMessage());
  }
}
