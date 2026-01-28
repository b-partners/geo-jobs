package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;
import static java.util.function.Predicate.not;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.model.OBB3DComputer;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
@RequiredArgsConstructor
public class ChimneyFixer implements Function<Collection<Plane3D>, List<Plane3D>> {
  private final double maxChimneyArea;
  private final OBB3DComputer obb3DComputer;
  private final Plane3DExtractionStepExporter exporter;
  private static final double ROOF_BUFFER_IN_METERS = 1;

  public ChimneyFixer(double maxChimneyArea, Plane3DExtractionStepExporter exporter) {
    this(maxChimneyArea, new OBB3DComputer(), exporter);
  }

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    var separated = separate(planes);
    List<Plane3D> result = new ArrayList<>(separated.others());

    var doExport = exporter != null;
    int i = 1;
    for (var chimney : separated.chimneys()) {
      var obb3D = obb3DComputer.apply(chimney);
      var fixed = chimney.toBuilder().delimitation(obb3D).area(null).build();

      result.add(fixed);

      if (doExport) {
        var subExporter = exporter.subSuffix(String.valueOf(i++));
        subExporter.export(CHIMNEY_FIXED_POLYGON, obb3D);
        subExporter.export(CHIMNEY_POLYGON, chimney.getDelimitation());
        subExporter.export(CHIMNEY_CONVEXE_POLYGON, chimney.getConvexDelimitation());
      }
    }

    return result;
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
              var bigDelimitationWithBuffer = bigDelimitation.buffer(ROOF_BUFFER_IN_METERS);
              if (!smallDelimitation.intersects(bigDelimitationWithBuffer)) {
                return false;
              }
              return getMinZ(bigDelimitation) < getMinZ(smallDelimitation);
            });
  }

  private static List<Plane3D> getPlanesWithSmallAreas(Collection<Plane3D> planes, double maxArea) {
    return planes.stream().filter(not(plane -> plane.getArea() > maxArea)).toList();
  }

  private static double getMinZ(Polygon polygon) {
    return Arrays.stream(polygon.getCoordinates())
        .mapToDouble(Coordinate::getZ)
        .min()
        .orElseThrow();
  }

  private record SeparatorResult(List<Plane3D> chimneys, List<Plane3D> others) {}
}
