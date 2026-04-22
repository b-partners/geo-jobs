package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.OnePlane3DExtractor;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import java.util.*;
import java.util.function.BiFunction;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector2D;

@Slf4j
public class RoofFaceToLidarAlignmentFixer
    implements BiFunction<DelimitedRoofPoints, List<Plane3D>, List<Plane3D>> {
  private final RoofFaceToLidarAlignmentFixerConf conf;
  private final OnePlane3DExtractor onePlane3DExtractor;

  public RoofFaceToLidarAlignmentFixer(Plane3DExtractorConf roofFaceExtractorConf) {
    this.conf = roofFaceExtractorConf.roofFaceToLidarAlignmentFixerConf();
    this.onePlane3DExtractor =
        new OnePlane3DExtractor(
            roofFaceExtractorConf.toBuilder().doSkinnyArmRemover(false).build());
  }

  @Override
  public List<Plane3D> apply(DelimitedRoofPoints delimitedRoofPoints, List<Plane3D> planes) {
    var current = planes;
    var roofFaceWithMaxAreaIndex = getRoofFaceWithMaxAreaIndex(planes);
    var refItem = delimitedRoofPoints.getItems()[roofFaceWithMaxAreaIndex];
    var refPoints = refItem.getPoints();

    for (int j = 0; j < conf.maxStepCount(); j++) {
      var refPlane = current.get(roofFaceWithMaxAreaIndex);
      var t = getBestTranslation(refPlane, refPoints);
      if (t.isEmpty()) {
        break;
      }
      log.info("STEP_INDEX={}", j);
      current = current.stream().map(p -> translate(p, t.get())).toList();
    }

    return current;
  }

  private Optional<Vector2D> getBestTranslation(
      Plane3D plane, Collection<LasPointGeometry> points) {
    var vectors = getVectors(conf.stepAngle(), conf.stepLength());

    Map<Vector2D, Integer> count = new HashMap<>();
    for (var vector : vectors) {
      count.put(vector, getBestCount(vector, plane, points));
    }

    var best = count.entrySet().stream().max(comparingDouble(Map.Entry::getValue)).orElseThrow();
    if (best.getValue() < conf.minScore()) {
      return Optional.empty();
    }
    return Optional.of(best.getKey());
  }

  private static Plane3D translate(Plane3D plane, Vector2D translation) {
    var delimitation = plane.getDelimitation();
    var coordinates = delimitation.getCoordinates();
    var translated = new Coordinate[coordinates.length];
    var tx = translation.getX();
    var ty = translation.getY();

    for (int i = 0; i < translated.length; i++) {
      var initial = coordinates[i];
      translated[i] = new Coordinate(initial.getX() + tx, initial.getY() + ty);
    }

    var translated2DPolygon = geometryFactory.createPolygon(translated);
    var translated3DPolygon = project(plane, translated2DPolygon);
    return plane.toBuilder().convexDelimitation(null).delimitation(translated3DPolygon).build();
  }

  private int getBestCount(Vector2D vector, Plane3D plane, Collection<LasPointGeometry> points) {
    var translated = translate(plane, vector);
    var inliers = points.stream().filter(translated.getDelimitation()::contains).collect(toSet());
    var newPlane = onePlane3DExtractor.apply(inliers, null).plane();

    var added = new HashSet<>(newPlane.getPoints());
    added.removeAll(plane.getPoints());

    var removed = new HashSet<>(plane.getPoints());
    removed.removeAll(newPlane.getPoints());

    return added.size() * conf.addedPointsFactor() - removed.size();
  }

  public static List<Vector2D> getVectors(int angleStep, double distance) {
    List<Vector2D> vectors = new ArrayList<>();
    for (int angle = 0; angle < 360; angle += angleStep) {
      var rad = Math.toRadians(angle);
      var dx = distance * Math.cos(rad);
      var dy = distance * Math.sin(rad);
      vectors.add(new Vector2D(dx, dy));
    }
    return vectors;
  }

  private static int getRoofFaceWithMaxAreaIndex(List<Plane3D> planes) {
    double maxArea = 0;
    int roofFacesWithMaxAreaIndex = 0;

    for (int i = 0; i < planes.size(); i++) {
      var area = planes.get(i).get2DArea();
      if (area > maxArea) {
        maxArea = area;
        roofFacesWithMaxAreaIndex = i;
      }
    }
    return roofFacesWithMaxAreaIndex;
  }

  @Builder(toBuilder = true)
  public record RoofFaceToLidarAlignmentFixerConf(
      int minScore, int stepAngle, int maxStepCount, int addedPointsFactor, double stepLength) {}
}
