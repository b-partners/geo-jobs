package app.bpartners.geojobs.service.cityjson;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.planes.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.utils.lidar.LidarRoofsAnalysisProcessorCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

class Plane3DExtractionDebuggerTest {
  private static final String LAMBERT_93 = "EPSG:2143";
  private static final String EXPORT_OUTPUT_FOLDER = "/home/ricka/export_city_jsons_output";
  private static final Plane3DExtractionStepExporter exporter =
      new Plane3DExtractionStepExporter(
          new ObjectMapper(), new File(EXPORT_OUTPUT_FOLDER), LAMBERT_93, "1");

  private static final Plane3DExtractorConf conf =
      Plane3DExtractorConf.getDefault().toBuilder().build();

  private static final LidarDataToCityJsonProcessor cityJsonProcessor =
      new LidarDataToCityJsonProcessor(new CityJsonFactory(), exporter);

  private static final LidarRoofsAnalysisProcessorCreator processorCreator =
      new LidarRoofsAnalysisProcessorCreator();

  @Test
  void export() {
    var roofsGeometries = Set.of(roofGeometry());
    var processor = processorCreator.create(roofsGeometries);

    var result = processor.from(roofsGeometries);

    cityJsonProcessor.apply("debug_city_jsons", result, conf);
  }

  private static Geometry roofGeometry1() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }

  private static Geometry roofGeometry() {
    var roofCoordinates =
        new Coordinate[] {
          new Coordinate(-1.6863076509379198, 48.12266055382972),
          new Coordinate(-1.6863304670840478, 48.12263390018964),
          new Coordinate(-1.6863318930931257, 48.12259011203619),
          new Coordinate(-1.6862948168553942, 48.122562506442335),
          new Coordinate(-1.68625488859945, 48.122557746855335),
          new Coordinate(-1.6860210230986468, 48.12253204507971),
          new Coordinate(-1.6859568526866724, 48.122513958637),
          new Coordinate(-1.6859867988782185, 48.12246826654339),
          new Coordinate(-1.6859212024580188, 48.12243780512489),
          new Coordinate(-1.6860851935097116, 48.12226931507837),
          new Coordinate(-1.6859169244299608, 48.12222362276714),
          new Coordinate(-1.6857458033319688, 48.12243399744648),
          new Coordinate(-1.685775749524339, 48.122440660884024),
          new Coordinate(-1.6857229871858692, 48.12251586247325),
          new Coordinate(-1.6858156777802549, 48.12255393918585),
          new Coordinate(-1.685804269707603, 48.1225663141112),
          new Coordinate(-1.6858812742012503, 48.12260629461926),
          new Coordinate(-1.6859511486495649, 48.12263390018964),
          new Coordinate(-1.6860281531440364, 48.12264056360124),
          new Coordinate(-1.6863076509379198, 48.12266055382972)
        };
    return geometryFactory.createPolygon(roofCoordinates);
  }
}
