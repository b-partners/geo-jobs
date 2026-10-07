package app.bpartners.geojobs.service.cityjson;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ROOF_SEGMENT_FACE_DELIMITATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.model.lidar.planes.model.RoofPointsDelimitationTransformer;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.service.lidar.PointsExtractionResult;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import app.bpartners.geojobs.service.lidar.api.WalloniaBoundaryChecker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class LidarDataToCityJsonProcessorGroundOnlyTest {
  private static final double GROUND_Z = 100;
  private static final double ROOF_Z = 110;

  private final LidarDataToCityJsonProcessor subject =
      new LidarDataToCityJsonProcessor(
          new CityJsonFactory(), new SwissBoundaryChecker(), new WalloniaBoundaryChecker());

  @Test
  void roof_without_lidar_points_is_rendered_as_ground_only() {
    var roof = segmentFaceRoof(square(0, 0, 10));
    addGroundPoints(roof);

    var surfaces = surfaceTypes(subject.apply("ground-only", resultOf(roof)));

    assertEquals(List.of("GroundSurface"), surfaces);
  }

  @Test
  void face_without_lidar_points_is_rendered_as_ground_while_others_keep_their_roof() {
    var faceWithPoints = square(0, 0, 10);
    var faceWithoutPoints = square(10, 0, 10);
    var roof = segmentFaceRoof(faceWithPoints, faceWithoutPoints);
    addGroundPoints(roof);
    for (double x = 0.5; x < 10; x += 0.5) {
      for (double y = 0.5; y < 10; y += 0.5) {
        roof.addRoofPointIfInside(new LasPointGeometry(x, y, ROOF_Z));
      }
    }

    var surfaces = surfaceTypes(subject.apply("partial", resultOf(roof)));

    assertEquals(1, surfaces.stream().filter("RoofSurface"::equals).count());
    assertEquals(2, surfaces.stream().filter("GroundSurface"::equals).count());
    assertFalse(surfaces.stream().filter("WallSurface"::equals).toList().isEmpty());
  }

  private static void addGroundPoints(DelimitedRoofPoints roof) {
    var envelope = roof.getGroundEnvelope();
    for (double x = envelope.getMinX(); x < envelope.getMaxX(); x += 1) {
      roof.addGroundPointIfInside(new LasPointGeometry(x, envelope.getMinY(), GROUND_Z));
    }
  }

  private static DelimitedRoofPoints segmentFaceRoof(Polygon... faces) {
    var originalInEPSG4326 = square(2.24, 48.82, 0.0001);
    return new DelimitedRoofPoints(
        ROOF_SEGMENT_FACE_DELIMITATION,
        originalInEPSG4326,
        faces,
        new RoofPointsDelimitationTransformer(0));
  }

  private static PointsExtractionResult resultOf(DelimitedRoofPoints roof) {
    var data = new HashMap<org.locationtech.jts.geom.Envelope, DelimitedRoofPoints>();
    data.put(roof.getOriginalInEPSG4336().getEnvelopeInternal(), roof);
    return new PointsExtractionResult(data);
  }

  private static Polygon square(double x, double y, double size) {
    return geometryFactory.createPolygon(
        new Coordinate[] {
          new Coordinate(x, y),
          new Coordinate(x + size, y),
          new Coordinate(x + size, y + size),
          new Coordinate(x, y + size),
          new Coordinate(x, y)
        });
  }

  @SneakyThrows
  private static List<String> surfaceTypes(java.io.File cityJson) {
    var root = new ObjectMapper().readTree(cityJson);
    var types = new ArrayList<String>();
    for (JsonNode object : root.get("CityObjects")) {
      for (JsonNode geometry : object.path("geometry")) {
        for (JsonNode surface : geometry.path("semantics").path("surfaces")) {
          types.add(surface.get("type").asText());
        }
      }
    }
    assertTrue(!types.isEmpty() || root.get("CityObjects").size() > 0);
    return types;
  }
}
