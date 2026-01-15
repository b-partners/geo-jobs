package app.bpartners.geojobs.model.lidar.planes;

import java.util.*;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;

@RequiredArgsConstructor
public class Plane3DMerger implements UnaryOperator<Collection<Plane3D>> {
  private final double epsilonSlope;
  private final double epsilonDistance;

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    List<Plane3D> result = new ArrayList<>();
    Set<Plane3D> visited = new HashSet<>();

    for (var p1 : planes) {
      if (visited.contains(p1)) {
        continue;
      }

      var merged = p1;
      for (var p2 : planes) {
        if (p1 == p2 || visited.contains(p2)) {
          continue;
        }

        if (shouldMerge(p1, p2)) {
          merged = merged.merge(p2);
          visited.add(p2);
        }
      }

      result.add(merged);
      visited.add(p1);
    }

    return result;
  }

  private boolean shouldMerge(Plane3D p1, Plane3D p2) {
    if (!isXYClose(p1, p2)) {
      return false;
    }

    if (!isAngleClose(p1, p2)) {
      return false;
    }

    return isHeightClose(p1, p2);
  }

  private boolean isAngleClose(Plane3D p1, Plane3D p2) {
    double dot = p1.getA() * p2.getA() + p1.getB() * p2.getB() + p1.getC() * p2.getC();
    double mag1 = Math.sqrt(p1.getA() * p1.getA() + p1.getB() * p1.getB() + p1.getC() * p1.getC());
    double mag2 = Math.sqrt(p2.getA() * p2.getA() + p2.getB() * p2.getB() + p2.getC() * p2.getC());
    double cosTheta = dot / (mag1 * mag2);
    double angleDeg = Math.toDegrees(Math.acos(Math.min(Math.abs(cosTheta), 1.0)));

    return angleDeg <= epsilonSlope;
  }

  private boolean isXYClose(Plane3D p1, Plane3D p2) {
    var bufferXY = p1.getDelimitation().buffer(epsilonDistance);
    return bufferXY.intersects(p2.getDelimitation());
  }

  private boolean isHeightClose(Plane3D p1, Plane3D p2) {
    double zMin1 =
        Arrays.stream(p1.getDelimitation().getCoordinates())
            .mapToDouble(Coordinate::getZ)
            .min()
            .orElse(0);

    double zMax1 =
        Arrays.stream(p1.getDelimitation().getCoordinates())
            .mapToDouble(Coordinate::getZ)
            .max()
            .orElse(0);

    double zMin2 =
        Arrays.stream(p2.getDelimitation().getCoordinates())
            .mapToDouble(Coordinate::getZ)
            .min()
            .orElse(0);

    double zMax2 =
        Arrays.stream(p2.getDelimitation().getCoordinates())
            .mapToDouble(Coordinate::getZ)
            .max()
            .orElse(0);

    return !(zMax1 + epsilonDistance < zMin2 || zMax2 + epsilonDistance < zMin1);
  }
}
