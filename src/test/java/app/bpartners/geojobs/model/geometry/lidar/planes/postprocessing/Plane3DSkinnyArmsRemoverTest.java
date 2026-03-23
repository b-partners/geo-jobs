package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.AFTER_REMOVING_SKINNY_ARM;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.PolygonSkinnyArmRemover;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.PolygonSkinnyArmRemover.PolygonSkinnyArmRemoverConf;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
class PolygonSkinnyArmsRemoverTest {
  private static final PolygonSkinnyArmRemoverConf conf =
      Plane3DExtractorConf.getDefault().polygonSkinnyArmRemoverConf();
  private static final File OUTPUT_DIRECTORY = createTempDirectory();
  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), OUTPUT_DIRECTORY, "EPSG:2154", "");
  private static final PolygonSkinnyArmRemover subject =
      new PolygonSkinnyArmRemover(conf, exporter);

  @BeforeAll
  static void setup() {
    log.info("Output Folder={}", OUTPUT_DIRECTORY.getAbsolutePath());
  }

  @Test
  void should_remove_anything_when_polygon_is_too_small() {
    var polygon = mock(Polygon.class);

    when(polygon.getArea()).thenReturn(conf.minAreaToCheck() - 1);

    var actual = subject.apply(polygon);

    assertSame(polygon, actual);
  }

  @Test
  void should_remove_long_line_as_much_as_possible() {
    var polygon = polygonWithSkinnyArm();
    var expected = polygonWithoutSkinnyArm();

    var actual = subject.apply(polygon);
    var subExporter = exporter.subSuffix("CASE_1");
    subExporter.export(AFTER_REMOVING_SKINNY_ARM, actual);

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  @Test
  void should_remove_long_line_as_much_as_possible_2() {
    var polygon = polygonWithSkinnyArm2();
    var expected = polygonWithoutSkinnyArm2();

    var actual = subject.apply(polygon);
    var subExporter = exporter.subSuffix("CASE_2");
    subExporter.export(AFTER_REMOVING_SKINNY_ARM, actual);

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  @Test
  void should_remove_long_line_as_much_as_possible_3() {
    var polygon = polygonWithSkinnyArm3();
    var expected = polygonWithoutSkinnyArm3();

    var actual = subject.apply(polygon);
    var subExporter = exporter.subSuffix("CASE_3");
    subExporter.export(AFTER_REMOVING_SKINNY_ARM, actual);

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  @Test
  void should_remove_anything() {
    var polygon = withoutSkinnyArm4();
    var expected = withoutSkinnyArm4();

    var actual = subject.apply(polygon);
    var subExporter = exporter.subSuffix("CASE_4");
    subExporter.export(AFTER_REMOVING_SKINNY_ARM, actual);

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  private static Polygon polygonWithSkinnyArm() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(379909.27, 6646657.86),
          new Coordinate(379908.62, 6646658.27),
          new Coordinate(379911.21, 6646664.11),
          new Coordinate(379916.34, 6646662.05),
          new Coordinate(379916.67, 6646661.05),
          new Coordinate(379912.71, 6646651.56),
          new Coordinate(379906.32, 6646644.09),
          new Coordinate(379910.86, 6646650.08),
          new Coordinate(379910.86, 6646651.75),
          new Coordinate(379908.28, 6646653.53),
          new Coordinate(379909.63, 6646655.34),
          new Coordinate(379909.27, 6646657.86)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon polygonWithoutSkinnyArm() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(379909.27, 6646657.86),
          new Coordinate(379908.62, 6646658.27),
          new Coordinate(379911.21, 6646664.11),
          new Coordinate(379916.34, 6646662.05),
          new Coordinate(379916.67, 6646661.05),
          new Coordinate(379912.71, 6646651.56),
          new Coordinate(379912.23096385575, 6646651.0),
          new Coordinate(379912.0, 6646651.0),
          new Coordinate(379911.0, 6646651.0),
          new Coordinate(379910.86, 6646651.0),
          new Coordinate(379910.86, 6646651.75),
          new Coordinate(379908.28, 6646653.53),
          new Coordinate(379909.63, 6646655.34),
          new Coordinate(379909.27, 6646657.86)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon polygonWithSkinnyArm2() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(566604.7000000001, 6273022.0),
          new Coordinate(566604.87, 6273023.86),
          new Coordinate(566605.4500000001, 6273024.51),
          new Coordinate(566606.39, 6273025.72),
          new Coordinate(566607.33, 6273026.66),
          new Coordinate(566608.3, 6273028.350000001),
          new Coordinate(566609.22, 6273028.99),
          new Coordinate(566609.8200000001, 6273029.79),
          new Coordinate(566610.21, 6273030.26),
          new Coordinate(566610.9400000001, 6273030.75),
          new Coordinate(566611.24, 6273031.04),
          new Coordinate(566612.02, 6273032.41),
          new Coordinate(566612.61, 6273034.25),
          new Coordinate(566612.64, 6273034.38),
          new Coordinate(566613.0, 6273033.72),
          new Coordinate(566613.49, 6273032.4),
          new Coordinate(566612.81, 6273031.7700000005),
          new Coordinate(566611.79, 6273030.66),
          new Coordinate(566611.28, 6273030.100000001),
          new Coordinate(566610.31, 6273028.86),
          new Coordinate(566609.5700000001, 6273028.09),
          new Coordinate(566609.99, 6273026.51),
          new Coordinate(566610.36, 6273026.22),
          new Coordinate(566610.79, 6273025.78),
          new Coordinate(566612.0, 6273024.69),
          new Coordinate(566612.66, 6273024.390000001),
          new Coordinate(566613.6900000001, 6273023.84),
          new Coordinate(566613.79, 6273023.79),
          new Coordinate(566614.54, 6273023.23),
          new Coordinate(566614.97, 6273022.66),
          new Coordinate(566615.0, 6273022.37),
          new Coordinate(566614.83, 6273021.99),
          new Coordinate(566614.41, 6273021.43),
          new Coordinate(566613.52, 6273020.21),
          new Coordinate(566612.5, 6273019.04),
          new Coordinate(566611.27, 6273017.68),
          new Coordinate(566611.18, 6273017.51),
          new Coordinate(566610.65, 6273016.5),
          new Coordinate(566610.26, 6273016.12),
          new Coordinate(566609.28, 6273015.12),
          new Coordinate(566608.68, 6273014.32),
          new Coordinate(566607.58, 6273012.95),
          new Coordinate(566606.7000000001, 6273011.84),
          new Coordinate(566606.28, 6273011.74),
          new Coordinate(566606.14, 6273011.890000001),
          new Coordinate(566605.6900000001, 6273012.4),
          new Coordinate(566605.58, 6273013.01),
          new Coordinate(566605.5, 6273014.49),
          new Coordinate(566605.49, 6273014.97),
          new Coordinate(566605.6, 6273015.7700000005),
          new Coordinate(566605.55, 6273016.9),
          new Coordinate(566605.26, 6273018.0),
          new Coordinate(566605.28, 6273019.0200000005),
          new Coordinate(566605.28, 6273019.21),
          new Coordinate(566605.27, 6273019.5200000005),
          new Coordinate(566605.08, 6273020.93),
          new Coordinate(566604.7000000001, 6273022.0)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon polygonWithoutSkinnyArm2() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(566604.7000000001, 6273022.0),
          new Coordinate(566604.87, 6273023.86),
          new Coordinate(566605.4500000001, 6273024.51),
          new Coordinate(566606.39, 6273025.72),
          new Coordinate(566607.33, 6273026.66),
          new Coordinate(566608.3, 6273028.350000001),
          new Coordinate(566608.0991124258, 6273028.0),
          new Coordinate(566609.0, 6273028.0),
          new Coordinate(566609.0, 6273027.0),
          new Coordinate(566609.8597468354, 6273027.0),
          new Coordinate(566609.99, 6273026.51),
          new Coordinate(566610.36, 6273026.22),
          new Coordinate(566610.79, 6273025.78),
          new Coordinate(566612.0, 6273024.69),
          new Coordinate(566612.66, 6273024.390000001),
          new Coordinate(566613.6900000001, 6273023.84),
          new Coordinate(566613.79, 6273023.79),
          new Coordinate(566614.54, 6273023.23),
          new Coordinate(566614.97, 6273022.66),
          new Coordinate(566615.0, 6273022.37),
          new Coordinate(566614.83, 6273021.99),
          new Coordinate(566614.41, 6273021.43),
          new Coordinate(566613.52, 6273020.21),
          new Coordinate(566612.5, 6273019.04),
          new Coordinate(566611.27, 6273017.68),
          new Coordinate(566611.18, 6273017.51),
          new Coordinate(566610.65, 6273016.5),
          new Coordinate(566610.26, 6273016.12),
          new Coordinate(566609.28, 6273015.12),
          new Coordinate(566608.68, 6273014.32),
          new Coordinate(566607.58, 6273012.95),
          new Coordinate(566606.7000000001, 6273011.84),
          new Coordinate(566606.28, 6273011.74),
          new Coordinate(566606.14, 6273011.890000001),
          new Coordinate(566605.6900000001, 6273012.4),
          new Coordinate(566605.58, 6273013.01),
          new Coordinate(566605.5, 6273014.49),
          new Coordinate(566605.49, 6273014.97),
          new Coordinate(566605.6, 6273015.7700000005),
          new Coordinate(566605.55, 6273016.9),
          new Coordinate(566605.26, 6273018.0),
          new Coordinate(566605.28, 6273019.0200000005),
          new Coordinate(566605.28, 6273019.21),
          new Coordinate(566605.27, 6273019.5200000005),
          new Coordinate(566605.08, 6273020.93),
          new Coordinate(566604.7000000001, 6273022.0) // fermeture du polygone
        };
    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon polygonWithSkinnyArm3() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(566604.7000000001, 6273022.0),
          new Coordinate(566604.65, 6273023.47),
          new Coordinate(566604.87, 6273023.86),
          new Coordinate(566605.17, 6273024.26),
          new Coordinate(566605.9400000001, 6273025.17),
          new Coordinate(566606.65, 6273026.11),
          new Coordinate(566607.71, 6273027.4),
          new Coordinate(566608.4400000001, 6273028.65),
          new Coordinate(566609.3, 6273029.54),
          new Coordinate(566609.77, 6273030.15),
          new Coordinate(566610.79, 6273031.75),
          new Coordinate(566610.98, 6273032.21),
          new Coordinate(566611.59, 6273032.5200000005),
          new Coordinate(566612.02, 6273032.41),
          new Coordinate(566612.3200000001, 6273032.12),
          new Coordinate(566612.27, 6273031.65),
          new Coordinate(566612.02, 6273031.2700000005),
          new Coordinate(566611.39, 6273030.5600000005),
          new Coordinate(566610.79, 6273029.86),
          new Coordinate(566610.23, 6273029.32),
          new Coordinate(566610.0700000001, 6273029.11),
          new Coordinate(566609.59, 6273028.48),
          new Coordinate(566609.29, 6273027.09),
          new Coordinate(566609.99, 6273026.51),
          new Coordinate(566610.36, 6273026.22),
          new Coordinate(566610.79, 6273025.78),
          new Coordinate(566612.0, 6273024.69),
          new Coordinate(566612.66, 6273024.390000001),
          new Coordinate(566613.6900000001, 6273023.84),
          new Coordinate(566614.54, 6273023.23),
          new Coordinate(566614.97, 6273022.66),
          new Coordinate(566615.05, 6273022.19),
          new Coordinate(566614.41, 6273021.43),
          new Coordinate(566613.52, 6273020.21),
          new Coordinate(566612.5, 6273019.04),
          new Coordinate(566611.27, 6273017.68),
          new Coordinate(566611.18, 6273017.51),
          new Coordinate(566610.65, 6273016.5),
          new Coordinate(566610.26, 6273016.12),
          new Coordinate(566609.28, 6273015.12),
          new Coordinate(566608.68, 6273014.32),
          new Coordinate(566607.58, 6273012.95),
          new Coordinate(566606.7000000001, 6273011.84),
          new Coordinate(566606.28, 6273011.74),
          new Coordinate(566606.14, 6273011.890000001),
          new Coordinate(566605.71, 6273012.65),
          new Coordinate(566605.74, 6273013.59),
          new Coordinate(566605.59, 6273014.7),
          new Coordinate(566605.5700000001, 6273015.36),
          new Coordinate(566605.55, 6273016.9),
          new Coordinate(566605.3, 6273018.7),
          new Coordinate(566605.28, 6273019.0200000005),
          new Coordinate(566605.28, 6273019.21),
          new Coordinate(566605.27, 6273019.5200000005),
          new Coordinate(566605.08, 6273020.93),
          new Coordinate(566604.7000000001, 6273022.0) // close polygon
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon polygonWithoutSkinnyArm3() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(566604.7000000001, 6273022.0),
          new Coordinate(566604.65, 6273023.47),
          new Coordinate(566604.87, 6273023.86),
          new Coordinate(566605.17, 6273024.26),
          new Coordinate(566605.9400000001, 6273025.17),
          new Coordinate(566606.65, 6273026.11),
          new Coordinate(566607.71, 6273027.4),
          new Coordinate(566608.0603999998, 6273028.0),
          new Coordinate(566609.0, 6273028.0),
          new Coordinate(566609.4864028776, 6273028.0),
          new Coordinate(566609.29, 6273027.09),
          new Coordinate(566609.99, 6273026.51),
          new Coordinate(566610.36, 6273026.22),
          new Coordinate(566610.79, 6273025.78),
          new Coordinate(566612.0, 6273024.69),
          new Coordinate(566612.66, 6273024.390000001),
          new Coordinate(566613.6900000001, 6273023.84),
          new Coordinate(566614.54, 6273023.23),
          new Coordinate(566614.97, 6273022.66),
          new Coordinate(566615.05, 6273022.19),
          new Coordinate(566614.41, 6273021.43),
          new Coordinate(566613.52, 6273020.21),
          new Coordinate(566612.5, 6273019.04),
          new Coordinate(566611.27, 6273017.68),
          new Coordinate(566611.18, 6273017.51),
          new Coordinate(566610.65, 6273016.5),
          new Coordinate(566610.26, 6273016.12),
          new Coordinate(566609.28, 6273015.12),
          new Coordinate(566608.68, 6273014.32),
          new Coordinate(566607.58, 6273012.95),
          new Coordinate(566606.7000000001, 6273011.84),
          new Coordinate(566606.28, 6273011.74),
          new Coordinate(566606.14, 6273011.890000001),
          new Coordinate(566605.71, 6273012.65),
          new Coordinate(566605.74, 6273013.59),
          new Coordinate(566605.59, 6273014.7),
          new Coordinate(566605.5700000001, 6273015.36),
          new Coordinate(566605.55, 6273016.9),
          new Coordinate(566605.3, 6273018.7),
          new Coordinate(566605.28, 6273019.0200000005),
          new Coordinate(566605.28, 6273019.21),
          new Coordinate(566605.27, 6273019.5200000005),
          new Coordinate(566605.08, 6273020.93),
          new Coordinate(566604.7000000001, 6273022.0) // fermeture du polygone
        };
    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon withoutSkinnyArm4() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(379905.07, 6646645.91),
          new Coordinate(379904.53, 6646646.28),
          new Coordinate(379911.01, 6646650.63),
          new Coordinate(379913.03, 6646650.41),
          new Coordinate(379914.76, 6646648.61),
          new Coordinate(379907.21, 6646642.94),
          new Coordinate(379905.2, 6646645.4),
          new Coordinate(379905.07, 6646645.91) // fermeture
        };

    return geometryFactory.createPolygon(coordinates);
  }
}
