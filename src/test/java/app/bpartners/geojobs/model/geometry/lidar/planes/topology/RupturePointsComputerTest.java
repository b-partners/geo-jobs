package app.bpartners.geojobs.model.geometry.lidar.planes.topology;

import static app.bpartners.geojobs.utils.lidar.LasPointGeometryLoaderUtils.readPointsFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier.RoofRelationClassifierConf;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofTopologyBuilder;
import app.bpartners.geojobs.model.lidar.planes.topology.RupturePointsComputer;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofTopology;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class RupturePointsComputerTest {
  private static final int TEST_COUNT = 10;
  private static final RupturePointsComputer subject = new RupturePointsComputer();
  private static final RoofTopologyBuilder topologyBuilder =
      new RoofTopologyBuilder(RoofRelationClassifierConf.builder().angleThresholdDeg(5).build());

  @Test
  void roof_2() {
    var pan1 = readPointsFromResources("cityjson/roofs/roof_2/pan_1.geojson");
    var pan2 = readPointsFromResources("cityjson/roofs/roof_2/pan_2.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var topology = getTopology(List.of(pan1, pan2));
      var rupture = topology.getRuptures()[0][1];
      assertTrue(rupture.getStartIntersection().isEmpty());
      assertTrue(rupture.getEndIntersection().isEmpty());
    }
  }

  @Test
  void roof_9() {
    var pan1 = readPointsFromResources("cityjson/roofs/roof_9/pan_1.geojson");
    var pan2 = readPointsFromResources("cityjson/roofs/roof_9/pan_2.geojson");
    var pan3 = readPointsFromResources("cityjson/roofs/roof_9/pan_3.geojson");
    var pan4 = readPointsFromResources("cityjson/roofs/roof_9/pan_4.geojson");

    for (int i = 0; i < TEST_COUNT; i++) {
      var topology = getTopology(List.of(pan1, pan2, pan3, pan4));

      var ruptureA = topology.getRuptures()[0][1];
      assertEquals(2, ruptureA.getStartIntersection().size());
      assertEquals(2, ruptureA.getEndIntersection().size());

      var ruptureB = topology.getRuptures()[1][0];
      assertEquals(2, ruptureB.getStartIntersection().size());
      assertEquals(2, ruptureB.getEndIntersection().size());
    }
  }

  private static RoofTopology getTopology(List<Collection<LasPointGeometry>> pans) {
    var planes = pans.stream().map(RuptureComputerTest::createPlane).toList();
    var topology = topologyBuilder.apply(planes);
    subject.accept(planes, topology);
    return topology;
  }
}
