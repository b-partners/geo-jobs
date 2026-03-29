package app.bpartners.geojobs.model.lidar.planes.topology;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.extend;
import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.*;
import static java.util.Comparator.comparingDouble;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofTopology;
import app.bpartners.geojobs.model.lidar.planes.topology.model.Rupture;
import java.util.*;
import java.util.function.BiConsumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

public class RupturePointsComputer implements BiConsumer<List<Plane3D>, RoofTopology> {
  @Override
  public void accept(List<Plane3D> planes, RoofTopology topology) {
    int n = planes.size();

    computeIntersectionRupturePoints(n, topology);
  }

  private static void computeIntersectionRupturePoints(int n, RoofTopology topology) {
    for (int i = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++) {
        var relation = topology.getRelations()[i][j];
        if (!S.equals(relation)) continue;

        computeSAndOIntersection(i, j, n, topology);
        computeSAndOIntersection(j, i, n, topology);
      }
    }
  }

  private static void computeSAndOIntersection(int i, int j, int n, RoofTopology topology) {
    var oRuptures = getORuptures(i, n, topology);
    if (oRuptures.isEmpty()) return;

    var sRupture = topology.getRuptures()[i][j];
    var extendedSLine = extend(sRupture.getLine(), 0.5);
    var indexedSLine = new LengthIndexedLine(sRupture.getLine());

    for (var o : oRuptures) {
      var extendedOLine = extend(o.getLine(), 0.5);
      if (!extendedSLine.intersects(extendedOLine)) continue;

      var intersection = extendedSLine.intersection(extendedOLine);
      if (!(intersection instanceof Point pointIntersection)) continue;

      var intersectionCoordinate = pointIntersection.getCoordinate();
      var intersectionLength = indexedSLine.project(intersectionCoordinate);
      var bCoordinate = getBCoordinate(sRupture.getLine(), o.getLine());
      var bPrimeLength = indexedSLine.project(bCoordinate);

      var aRupture = topology.getRuptures()[i][j];
      var bRupture = topology.getRuptures()[j][i];
      if (shouldAddInStart(bPrimeLength, intersectionLength)) {
        aRupture.getStartIntersection().add(intersectionCoordinate);
        bRupture.getStartIntersection().add(intersectionCoordinate);
      } else {
        aRupture.getEndIntersection().add(intersectionCoordinate);
        bRupture.getEndIntersection().add(intersectionCoordinate);
      }
    }
  }

  private static boolean shouldAddInStart(double bPrimeLength, double intersectionLength) {
    /*
     *       CASE_1
     *
     *           b
     *           |\
     *           | \ (O_Line)
     *           |  \
     *    x------b'--a-----x'  : S_Line
     *               ^
     *          intersection
     * ============================================================
     *       CASE_2
     *
     *               b
     *              /|
     *   (O_LINE)  / |
     *            /  |
     *    x------a---b'-------x'  : S_Line
     *           ^
     *      intersection
     * */
    return bPrimeLength < intersectionLength;
  }

  private static Coordinate getBCoordinate(LineString sLine, LineString oLine) {
    return Arrays.stream(oLine.getCoordinates())
        .max(comparingDouble(coordinate -> sLine.distance(new LasPointGeometry(coordinate))))
        .orElseThrow();
  }

  private static List<Rupture> getORuptures(int i, int n, RoofTopology topology) {
    List<Rupture> ruptures = new ArrayList<>();
    for (int j = i + 1; j < n; j++) {
      var relation = topology.getRelations()[i][j];
      if (!O_PLUS.equals(relation) && !O_MINUS.equals(relation)) continue;
      ruptures.add(topology.getRuptures()[i][j]);
    }
    return ruptures;
  }
}
