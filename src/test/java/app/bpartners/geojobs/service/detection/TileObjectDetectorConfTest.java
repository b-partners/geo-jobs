package app.bpartners.geojobs.service.detection;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TileObjectDetectorConfIT extends FacadeIT {
  @Autowired TileObjectDetectorConf subject;

  @Test
  void get_tile_detection_api_urls() {
    var actual = subject.getTileDetectionApiUrls();

    assertNotNull(actual);
    assertFalse(actual.isBlank());
  }
}
