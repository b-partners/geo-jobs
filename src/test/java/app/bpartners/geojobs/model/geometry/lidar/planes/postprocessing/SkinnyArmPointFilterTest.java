package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;

import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation.PlaneDelimitationConf;
import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.PolygonSkinnyArmRemover.PolygonSkinnyArmRemoverConf;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.SkinnyArmPointFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
class SkinnyArmPointFilterTest {
  private static final PolygonSkinnyArmRemoverConf conf =
      Plane3DExtractorConf.getDefault().polygonSkinnyArmRemoverConf();
  private static final PlaneDelimitationConf delimitationConf =
      Plane3DExtractorConf.getDefault().planeDelimitationConf();
  private static final SkinnyArmPointFilter subject =
      new SkinnyArmPointFilter(conf, delimitationConf);

  private static final File directory = createTempDirectory();
  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), directory, "EPSG:2143", "");

  @BeforeAll
  static void setup() {
    log.info("Output Folder = {}", directory.getAbsolutePath());
  }

  @Test
  void shouldRemoveSkinnyArm() {
    var points = readPointsFromResources("cityjson/points/concave_points.geojson");
    var subExporter = exporter.subSuffix("CASE_1");

    var actual = subject.apply(points, subExporter);
  }

  @Test
  void shouldRemoveSkinnyArm2() {
    var points = readPointsFromResources("cityjson/points/concave_points_2.geojson");
    var subExporter = exporter.subSuffix("CASE_2");

    var actual = subject.apply(points, subExporter);
  }
}
