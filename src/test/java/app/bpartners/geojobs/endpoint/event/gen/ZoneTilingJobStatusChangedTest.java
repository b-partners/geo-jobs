package app.bpartners.geojobs.endpoint.event.gen;

import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneTilingJobStatusChangedTest {

  @Test
  void testToString() {
    var statusChanged = new ZoneTilingJobStatusChanged();
    statusChanged.setOldJob(ZoneTilingJob.builder().id("oldId").build());
    statusChanged.setNewJob(ZoneTilingJob.builder().id("newId").build());

    List<Object> eventsAsObjects = List.of(statusChanged);
    var asString = eventsAsObjects.toString();

    assertTrue(asString.contains("oldId"));
    assertTrue(asString.contains("newId"));
  }
}