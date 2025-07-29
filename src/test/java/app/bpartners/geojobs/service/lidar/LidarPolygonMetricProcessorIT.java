package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.conf.FacadeIT;
import java.io.File;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Disabled
public class LidarPolygonMetricProcessorIT extends FacadeIT {
  @Autowired LidarPolygonMetricProcessor subject;

  @Test
  void compute_roof_slope() {
    var lidarTile1 =
        new File(
            getClass().getResource("/las/LHD_FXX_0451_6622_PTS_C_LAMB93_IGN69.copc.laz").getFile());
    var lidarTile2 =
        new File(
            getClass().getResource("/las/LHD_FXX_0451_6623_PTS_C_LAMB93_IGN69.copc.laz").getFile());

    var coordinates =
        new Coordinate[] {
          new Coordinate(-0.24943324176473425, 46.65206839265869),
          new Coordinate(-0.24951210176888594, 46.65188403099066),
          new Coordinate(-0.24884922057415793, 46.651836175306215),
          new Coordinate(-0.2488366486902862, 46.65201504551692),
          new Coordinate(-0.24943324176473425, 46.65206839265869)
        };

    var roofGeometry = geometryFactory.createPolygon(coordinates);
    var actual = subject.apply(roofGeometry, Set.of(lidarTile1, lidarTile2));

    var slope = actual.getSlopeInDegrees();
    var height = actual.getHeightInMeters();

    log.info("slope: {}, height: {}", slope, height);
  }
}
