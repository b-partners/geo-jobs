package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static java.util.function.Predicate.not;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class ChimneyFixer implements Function<Collection<Plane3D>, List<Plane3D>> {
  private final double maxChimneyArea;

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    var result = separate(planes);

    // TODO: fix chimneys

    return List.of();
  }

  private SeparatorResult separate(Collection<Plane3D> planes) {
    var planesWithBigAreas = getPlanesWithBigAreasSortedByArea(planes, maxChimneyArea);
    var planesWithSmallAreas = getPlanesWithSmallAreas(planes, maxChimneyArea);
    var result = new SeparatorResult(new ArrayList<>(), new ArrayList<>(planesWithBigAreas));

    for (var small : planesWithSmallAreas) {
      if (isAChimney(small, planesWithBigAreas)) {
        result.chimneys().add(small);
      } else {
        result.others().add(small);
      }
    }
    return result;
  }

  private static List<Plane3D> getPlanesWithBigAreasSortedByArea(
      Collection<Plane3D> planes, double maxArea) {
    return planes.stream()
        .filter(plane -> plane.getArea() > maxArea)
        .sorted(Comparator.comparingDouble(Plane3D::get2DArea).reversed())
        .toList();
  }

  private static boolean isAChimney(Plane3D small, Collection<Plane3D> bigPlanes) {
    var smallDelimitation = small.getDelimitation();

    return bigPlanes.stream()
        .anyMatch(
            big -> {
              var bigDelimitation = big.getDelimitation();
              if (!smallDelimitation.intersects(bigDelimitation)) {
                return false;
              }
              return getMinZ(bigDelimitation) < getMinZ(smallDelimitation);
            });
  }

  private static List<Plane3D> getPlanesWithSmallAreas(Collection<Plane3D> planes, double maxArea) {
    return planes
        .stream()
        .filter(not(plane -> plane.getArea() > maxArea))
        .toList();
  }

  private static double getMinZ(Polygon polygon) {
    return Arrays.stream(polygon.getCoordinates())
        .mapToDouble(Coordinate::getZ)
        .min()
        .orElseThrow();
  }

  private record SeparatorResult(List<Plane3D> chimneys, List<Plane3D> others) {}
}
