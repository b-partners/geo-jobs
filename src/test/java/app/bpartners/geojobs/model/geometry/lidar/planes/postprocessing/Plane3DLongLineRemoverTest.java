package app.bpartners.geojobs.model.geometry.lidar.planes.postprocessing;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.AFTER_LONG_LINE;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.BEFORE_LONG_LINE;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.lidar.planes.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.Plane3DLongLineRemover;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.Plane3DLongLineRemover.Plane3DLongLineRemoverConf;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
class Plane3DLongLineRemoverTest {
  private static final Plane3DLongLineRemoverConf conf =
      Plane3DExtractorConf.getDefault().plane3DLongLineRemoverConf();
  private static final Plane3DLongLineRemover subject = new Plane3DLongLineRemover(conf);
  private static final File OUTPUT_DIRECTORY = createTempDirectory();

  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), OUTPUT_DIRECTORY, "EPSG:2154", "");

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
    var polygon = polygonWithLongLine();
    var expected = polygonWithoutLongLine();

    var actual = subject.apply(polygon);
    var subExporter = exporter.subSuffix("CASE_1");
    subExporter.export(BEFORE_LONG_LINE, polygon);
    subExporter.export(AFTER_LONG_LINE, actual);

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  @Test
  void should_remove_long_line_as_much_as_possible_2() {
    var polygon = polygonWithLongLine2();
    var expected = polygonWithoutLongLine2();

    var actual = subject.apply(polygon);
    var subExporter = exporter.subSuffix("CASE_2");
    subExporter.export(BEFORE_LONG_LINE, polygon);
    subExporter.export(AFTER_LONG_LINE, actual);

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  @Test
  void should_remove_long_line_as_much_as_possible_3() {
    var polygon = polygonWithLongLine3();
    var expected = polygonWithoutLongLine3();

    var actual = subject.apply(polygon);
    var subExporter = exporter.subSuffix("CASE_3");
    subExporter.export(BEFORE_LONG_LINE, polygon);
    subExporter.export(AFTER_LONG_LINE, actual);

    assertTrue(expected.equalsExact(actual, 0.2));
  }

  private static Polygon polygonWithLongLine() {
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

  private static Polygon polygonWithoutLongLine() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(379908.6305524861, 6646654.0),
          new Coordinate(379909.0, 6646654.4953333335),
          new Coordinate(379909.37640883983, 6646655.0),
          new Coordinate(379909.63, 6646655.34),
          new Coordinate(379909.53571428574, 6646656.0),
          new Coordinate(379909.3928571429, 6646657.0),
          new Coordinate(379909.27, 6646657.86),
          new Coordinate(379909.0480487806, 6646658.0),
          new Coordinate(379909.0, 6646658.0303076925),
          new Coordinate(379908.62, 6646658.27),
          new Coordinate(379908.94375000015, 6646659.0),
          new Coordinate(379909.0, 6646659.126833976),
          new Coordinate(379909.3872431508, 6646660.0),
          new Coordinate(379909.83073630143, 6646661.0),
          new Coordinate(379910.0, 6646661.381660231),
          new Coordinate(379910.2742294521, 6646662.0),
          new Coordinate(379910.71772260265, 6646663.0),
          new Coordinate(379911.0, 6646663.6364864865),
          new Coordinate(379911.1612157533, 6646664.0),
          new Coordinate(379911.21, 6646664.11),
          new Coordinate(379911.48393203964, 6646664.0),
          new Coordinate(379912.0, 6646663.792768031),
          new Coordinate(379913.0, 6646663.391208577),
          new Coordinate(379913.9742233011, 6646663.0),
          new Coordinate(379914.0, 6646662.989649123),
          new Coordinate(379915.0, 6646662.588089668),
          new Coordinate(379916.0, 6646662.186530215),
          new Coordinate(379916.34, 6646662.05),
          new Coordinate(379916.35649999994, 6646662.0),
          new Coordinate(379916.67, 6646661.05),
          new Coordinate(379916.6491359326, 6646661.0),
          new Coordinate(379916.23185458384, 6646660.0),
          new Coordinate(379916.0, 6646659.4443686865),
          new Coordinate(379915.81457323505, 6646659.0),
          new Coordinate(379915.3972918863, 6646658.0),
          new Coordinate(379915.0, 6646657.04790404),
          new Coordinate(379914.98001053755, 6646657.0),
          new Coordinate(379914.56272918876, 6646656.0),
          new Coordinate(379914.14544784, 6646655.0),
          new Coordinate(379914.0, 6646654.651439394),
          new Coordinate(379913.7281664912, 6646654.0),
          new Coordinate(379913.3108851424, 6646653.0),
          new Coordinate(379913.0, 6646652.254974747),
          new Coordinate(379912.89360379364, 6646652.0),
          new Coordinate(379912.71, 6646651.56),
          new Coordinate(379912.23096385575, 6646651.0),
          new Coordinate(379912.0, 6646651.0),
          new Coordinate(379911.0, 6646651.0),
          new Coordinate(379910.86, 6646651.0),
          new Coordinate(379910.86, 6646651.75),
          new Coordinate(379910.4976404495, 6646652.0),
          new Coordinate(379910.0, 6646652.343333334),
          new Coordinate(379909.0482022475, 6646653.0),
          new Coordinate(379909.0, 6646653.033255815),
          new Coordinate(379908.28, 6646653.53),
          new Coordinate(379908.6305524861, 6646654.0)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon polygonWithLongLine2() {
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

  private static Polygon polygonWithoutLongLine2() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(566604.7000000001, 6273022.0),
          new Coordinate(566604.7913978494, 6273023.0),
          new Coordinate(566604.87, 6273023.86),
          new Coordinate(566604.9949230767, 6273024.0),
          new Coordinate(566605.0, 6273024.005689655),
          new Coordinate(566605.4500000001, 6273024.51),
          new Coordinate(566605.8306611573, 6273025.0),
          new Coordinate(566606.0, 6273025.217978723),
          new Coordinate(566606.39, 6273025.72),
          new Coordinate(566606.6700000002, 6273026.0),
          new Coordinate(566607.0, 6273026.33),
          new Coordinate(566607.33, 6273026.66),
          new Coordinate(566607.5251479289, 6273027.0),
          new Coordinate(566608.0, 6273027.827319588),
          new Coordinate(566608.0991124258, 6273028.0),
          new Coordinate(566609.0, 6273028.0),
          new Coordinate(566609.0, 6273027.0),
          new Coordinate(566609.8597468354, 6273027.0),
          new Coordinate(566609.99, 6273026.51),
          new Coordinate(566610.0, 6273026.502162162),
          new Coordinate(566610.36, 6273026.22),
          new Coordinate(566610.5750000001, 6273026.0),
          new Coordinate(566610.79, 6273025.78),
          new Coordinate(566611.0, 6273025.590826446),
          new Coordinate(566611.6558715601, 6273025.0),
          new Coordinate(566612.0, 6273024.69),
          new Coordinate(566612.66, 6273024.390000001),
          new Coordinate(566613.0, 6273024.208446602),
          new Coordinate(566613.3903636365, 6273024.0),
          new Coordinate(566613.6900000001, 6273023.84),
          new Coordinate(566613.79, 6273023.79),
          new Coordinate(566614.0, 6273023.6332),
          new Coordinate(566614.54, 6273023.23),
          new Coordinate(566614.7135087722, 6273023.0),
          new Coordinate(566614.97, 6273022.66),
          new Coordinate(566615.0, 6273022.37),
          new Coordinate(566614.8344736841, 6273022.0),
          new Coordinate(566614.83, 6273021.99),
          new Coordinate(566614.41, 6273021.43),
          new Coordinate(566614.0963114756, 6273021.0),
          new Coordinate(566614.0, 6273020.867977528),
          new Coordinate(566613.52, 6273020.21),
          new Coordinate(566613.336923077, 6273020.0),
          new Coordinate(566613.0, 6273019.613529412),
          new Coordinate(566612.5, 6273019.04),
          new Coordinate(566612.4638235294, 6273019.0),
          new Coordinate(566612.0, 6273018.487154472),
          new Coordinate(566611.559411765, 6273018.0),
          new Coordinate(566611.27, 6273017.68),
          new Coordinate(566611.18, 6273017.51),
          new Coordinate(566611.0, 6273017.166981132),
          new Coordinate(566610.9123762377, 6273017.0),
          new Coordinate(566610.65, 6273016.5),
          new Coordinate(566610.26, 6273016.12),
          new Coordinate(566610.1423999999, 6273016.0),
          new Coordinate(566610.0, 6273015.8546938775),
          new Coordinate(566609.28, 6273015.12),
          new Coordinate(566609.19, 6273015.0),
          new Coordinate(566609.0, 6273014.746666667),
          new Coordinate(566608.68, 6273014.32),
          new Coordinate(566608.4230656932, 6273014.0),
          new Coordinate(566608.0, 6273013.473090909),
          new Coordinate(566607.6201459852, 6273013.0),
          new Coordinate(566607.58, 6273012.95),
          new Coordinate(566607.0, 6273012.218409091),
          new Coordinate(566606.826846847, 6273012.0),
          new Coordinate(566606.7000000001, 6273011.84),
          new Coordinate(566606.28, 6273011.74),
          new Coordinate(566606.14, 6273011.890000001),
          new Coordinate(566606.042941177, 6273012.0),
          new Coordinate(566606.0, 6273012.048666667),
          new Coordinate(566605.6900000001, 6273012.4),
          new Coordinate(566605.5818032786, 6273013.0),
          new Coordinate(566605.58, 6273013.01),
          new Coordinate(566605.5264864864, 6273014.0),
          new Coordinate(566605.5, 6273014.49),
          new Coordinate(566605.49, 6273014.97),
          new Coordinate(566605.494125, 6273015.0),
          new Coordinate(566605.6, 6273015.7700000005),
          new Coordinate(566605.5898230089, 6273016.0),
          new Coordinate(566605.55, 6273016.9),
          new Coordinate(566605.5236363638, 6273017.0),
          new Coordinate(566605.26, 6273018.0),
          new Coordinate(566605.2796078431, 6273019.0),
          new Coordinate(566605.28, 6273019.0200000005),
          new Coordinate(566605.28, 6273019.21),
          new Coordinate(566605.27, 6273019.5200000005),
          new Coordinate(566605.205319149, 6273020.0),
          new Coordinate(566605.08, 6273020.93),
          new Coordinate(566605.0551401868, 6273021.0),
          new Coordinate(566605.0, 6273021.155263158),
          new Coordinate(566604.7000000001, 6273022.0)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private static Polygon polygonWithLongLine3() {
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

  private static Polygon polygonWithoutLongLine3() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(566604.6659863946, 6273023.0),
          new Coordinate(566604.65, 6273023.47),
          new Coordinate(566604.87, 6273023.86),
          new Coordinate(566604.9749999999, 6273024.0),
          new Coordinate(566605.0, 6273024.033333333),
          new Coordinate(566605.17, 6273024.26),
          new Coordinate(566605.7961538463, 6273025.0),
          new Coordinate(566605.9400000001, 6273025.17),
          new Coordinate(566606.0, 6273025.24943662),
          new Coordinate(566606.5669148935, 6273026.0),
          new Coordinate(566606.65, 6273026.11),
          new Coordinate(566607.0, 6273026.535943396),
          new Coordinate(566607.3813178291, 6273027.0),
          new Coordinate(566607.71, 6273027.4),
          new Coordinate(566608.0, 6273027.896575343),
          new Coordinate(566608.0603999998, 6273028.0),
          new Coordinate(566609.0, 6273028.0),
          new Coordinate(566609.4864028776, 6273028.0),
          new Coordinate(566609.29, 6273027.09),
          new Coordinate(566609.3986206895, 6273027.0),
          new Coordinate(566609.99, 6273026.51),
          new Coordinate(566610.0, 6273026.502162162),
          new Coordinate(566610.36, 6273026.22),
          new Coordinate(566610.5750000001, 6273026.0),
          new Coordinate(566610.79, 6273025.78),
          new Coordinate(566611.0, 6273025.590826446),
          new Coordinate(566611.6558715601, 6273025.0),
          new Coordinate(566612.0, 6273024.69),
          new Coordinate(566612.66, 6273024.390000001),
          new Coordinate(566613.0, 6273024.208446602),
          new Coordinate(566613.3903636365, 6273024.0),
          new Coordinate(566613.6900000001, 6273023.84),
          new Coordinate(566614.0, 6273023.617529412),
          new Coordinate(566614.54, 6273023.23),
          new Coordinate(566614.7135087722, 6273023.0),
          new Coordinate(566614.97, 6273022.66),
          new Coordinate(566615.0, 6273022.483750001),
          new Coordinate(566615.05, 6273022.19),
          new Coordinate(566615.0, 6273022.130625),
          new Coordinate(566614.8899999999, 6273022.0),
          new Coordinate(566614.41, 6273021.43),
          new Coordinate(566614.0963114756, 6273021.0),
          new Coordinate(566614.0, 6273020.867977528),
          new Coordinate(566613.52, 6273020.21),
          new Coordinate(566613.336923077, 6273020.0),
          new Coordinate(566613.0, 6273019.613529412),
          new Coordinate(566612.5, 6273019.04),
          new Coordinate(566612.4638235294, 6273019.0),
          new Coordinate(566612.0, 6273018.487154472),
          new Coordinate(566611.559411765, 6273018.0),
          new Coordinate(566611.27, 6273017.68),
          new Coordinate(566611.18, 6273017.51),
          new Coordinate(566611.0, 6273017.166981132),
          new Coordinate(566610.9123762377, 6273017.0),
          new Coordinate(566610.65, 6273016.5),
          new Coordinate(566610.26, 6273016.12),
          new Coordinate(566610.1423999999, 6273016.0),
          new Coordinate(566610.0, 6273015.8546938775),
          new Coordinate(566609.28, 6273015.12),
          new Coordinate(566609.19, 6273015.0),
          new Coordinate(566609.0, 6273014.746666667),
          new Coordinate(566608.68, 6273014.32),
          new Coordinate(566608.4230656932, 6273014.0),
          new Coordinate(566608.0, 6273013.473090909),
          new Coordinate(566607.6201459852, 6273013.0),
          new Coordinate(566607.58, 6273012.95),
          new Coordinate(566607.0, 6273012.218409091),
          new Coordinate(566606.826846847, 6273012.0),
          new Coordinate(566606.7000000001, 6273011.84),
          new Coordinate(566606.28, 6273011.74),
          new Coordinate(566606.14, 6273011.890000001),
          new Coordinate(566606.0777631582, 6273012.0),
          new Coordinate(566606.0, 6273012.137441861),
          new Coordinate(566605.71, 6273012.65),
          new Coordinate(566605.7211702127, 6273013.0),
          new Coordinate(566605.74, 6273013.59),
          new Coordinate(566605.6845945945, 6273014.0),
          new Coordinate(566605.59, 6273014.7),
          new Coordinate(566605.5809090909, 6273015.0),
          new Coordinate(566605.5700000001, 6273015.36),
          new Coordinate(566605.5616883117, 6273016.0),
          new Coordinate(566605.55, 6273016.9),
          new Coordinate(566605.5361111112, 6273017.0),
          new Coordinate(566605.3972222223, 6273018.0),
          new Coordinate(566605.3, 6273018.7),
          new Coordinate(566605.28125, 6273019.0),
          new Coordinate(566605.28, 6273019.0200000005),
          new Coordinate(566605.28, 6273019.21),
          new Coordinate(566605.27, 6273019.5200000005),
          new Coordinate(566605.205319149, 6273020.0),
          new Coordinate(566605.08, 6273020.93),
          new Coordinate(566605.0551401868, 6273021.0),
          new Coordinate(566605.0, 6273021.155263158),
          new Coordinate(566604.7000000001, 6273022.0),
          new Coordinate(566604.6659863946, 6273023.0)
        };

    return geometryFactory.createPolygon(coordinates);
  }
}
