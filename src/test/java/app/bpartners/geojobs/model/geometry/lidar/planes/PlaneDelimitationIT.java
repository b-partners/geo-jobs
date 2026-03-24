package app.bpartners.geojobs.model.geometry.lidar.planes;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;

import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation;
import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation.PlaneDelimitationConf;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf;
import app.bpartners.geojobs.model.lidar.planes.conf.RangedConf.IntegerRangedConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Slf4j
@Disabled
class PlaneDelimitationIT {
  private static final File directory = createTempDirectory();
  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), directory, "EPSG:2143", "");
  private static final PlaneDelimitationConf conf =
      PlaneDelimitationConf.builder()
          .concaveRatio(
              RangedConf.from(
                  new IntegerRangedConf<>(Integer.MIN_VALUE, 100, 0.2),
                  new IntegerRangedConf<>(101, Integer.MAX_VALUE, 0.09)))
          .simplificationEpsilon(0.3)
          .build();

  @BeforeAll
  static void setup() {
    log.info("Output Folder = {}", directory.getAbsolutePath());
  }

  @Test
  void fromConcavePoints() {
    var points = readPointsFromResources("cityjson/points/concave_points.geojson");

    var subExporter = exporter.subSuffix("CASE_1");
    var delimitation = new PlaneDelimitation(conf, points, subExporter);
    var actual = delimitation.getPolygon();
  }

  @Test
  void fromConcavePoints2() {
    var points = readPointsFromResources("cityjson/points/concave_points_2.geojson");

    var subExporter = exporter.subSuffix("CASE_2");
    var delimitation = new PlaneDelimitation(conf, points, subExporter);
    var actual = delimitation.getPolygon();
  }

  @Test
  void fromConcavePoints3() {
    var points = readPointsFromResources("cityjson/points/concave_points_3.geojson");

    var subExporter = exporter.subSuffix("CASE_3");
    var delimitation = new PlaneDelimitation(conf, points, subExporter);
    var actual = delimitation.getPolygon();
  }
}
