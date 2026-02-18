package app.bpartners.geojobs.service.cityjson;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.planes.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.utils.lidar.LidarRoofsAnalysisProcessorCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Slf4j
class Plane3DExtractionDebuggerTest {
  private static final String LAMBERT_93 = "EPSG:2143";
  private static final File EXPORT_OUTPUT_FOLDER = createTempDirectory();

  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(new ObjectMapper(), EXPORT_OUTPUT_FOLDER, LAMBERT_93, "1");

  private static final Plane3DExtractorConf conf =
      Plane3DExtractorConf.getDefault().toBuilder().build();

  private static final LidarDataToCityJsonProcessor cityJsonProcessor =
      new LidarDataToCityJsonProcessor(new CityJsonFactory(EXPORT_OUTPUT_FOLDER), exporter);

  private static final LidarRoofsAnalysisProcessorCreator processorCreator =
      new LidarRoofsAnalysisProcessorCreator();

  @Test
  void export() {
    log.info("Output Folder = {}", EXPORT_OUTPUT_FOLDER);
    var roofsGeometries = Set.of(roofGeometry2());
    var processor = processorCreator.create(roofsGeometries);

    var result = processor.from(roofsGeometries);

    cityJsonProcessor.apply("debug_city_jsons", result, conf);
  }

  private static Geometry roofGeometry2() {
    var roof2Coordinates =
        new Coordinate[] {
          new Coordinate(2.2431823989819577, 48.82457400501346),
          new Coordinate(2.243242034747283, 48.82446145324346),
          new Coordinate(2.24349250495996, 48.824520346643),
          new Coordinate(2.243502444253778, 48.8244941718074),
          new Coordinate(2.243595873618915, 48.824520346643),
          new Coordinate(2.2435342499950366, 48.82464598566398),
          new Coordinate(2.2431823989819577, 48.82457400501346)
        };
    return geometryFactory.createPolygon(roof2Coordinates);
  }
}
