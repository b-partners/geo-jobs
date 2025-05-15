package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.LineInt;
import app.bpartners.geojobs.model.geometry.TwoLineInt;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnifiedRoute;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class BoundaryMerger implements Function<Set<LatLonPolygon>, Set<LatLonPolygon>> {
  private final TilingConf tilingConf;
  private final UnionConf unionConf;
  private final NeighbourHoodHandler neighbourHoodHandler;
  private final PolygonPrettier polygonPrettier;
  private final MergeConf mergeConf;
  private final ExecutorService executorService =
      newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() / 2));

  public BoundaryMerger(
      TilingConf tilingConf,
      UnionConf unionConf,
      PrettyConf prettyConf,
      MergeConf mergeConf,
      int neighbourhoodTileDistance) {
    this.tilingConf = tilingConf;
    this.unionConf = unionConf;
    this.neighbourHoodHandler = new NeighbourHoodHandler(neighbourhoodTileDistance);
    this.polygonPrettier = new PolygonPrettier(prettyConf);
    this.mergeConf = mergeConf;
  }

  @Override
  public Set<LatLonPolygon> apply(Set<LatLonPolygon> latLonPolygons) {
    var tiledPolygons =
        latLonPolygons.stream().map(p -> p.tiledPolygon(tilingConf)).collect(toSet());

    /*
     * * * * * * * * * * * * * * * * * * * * * *
     * Uncomment when zone area is up to 2km2  *
     * * * * * * * * * * * * * * * * * * * * * *
     **/

    // var polygonsByNeighbourhood = neighbourHoodHandler.aroundN(tiledPolygons);
    /**
     * List<Future<Set<TiledPolygon>>> futures; try { futures = executorService.invokeAll(
     * polygonsByNeighbourhood.stream() .map( neighbourhoodPolygons ->
     * ((Callable<Set<TiledPolygon>>) () -> applyBoundaryMerge(neighbourhoodPolygons)))
     * .collect(toSet())); } catch (InterruptedException e) { throw new RuntimeException(e); }
     *
     * <p>return futures.stream().flatMap(this::futureStream).collect(toSet()).stream()
     * .map(TiledPolygon::latLonPolygon) .collect(toSet());
     */
    return applyBoundaryMerge(tiledPolygons).stream()
        .map(TiledPolygon::latLonPolygon)
        .collect(toSet());
  }

  public Set<LatLonPolygon> from(Set<TiledPolygon> tiledPolygons) {
    var latLonPolygons = tiledPolygons.stream().map(TiledPolygon::latLonPolygon).collect(toSet());
    return apply(latLonPolygons);
  }

  private Stream<TiledPolygon> futureStream(Future<Set<TiledPolygon>> future) {
    try {
      return future.get().stream();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<TiledPolygon> applyBoundaryMerge(Set<TiledPolygon> tiledPolygons) {
    var groupedByType = tiledPolygons.stream().collect(groupingBy(TiledPolygon::type));
    var newTiledPolygons = new HashSet<Set<TiledPolygon>>();
    for (var group : groupedByType.values()) {
      var newGroupedTiledPolygons = new HashSet<TiledPolygon>();
      var size = group.size();
      var toSkip = new HashSet<>();
      for (int i = 0; i < size; i++) {
        var base = group.get(i);
        var toUnify = new HashSet<Polygon>();

        if (toSkip.contains(i)) {
          continue;
        }

        for (int j = i + 1; j < size; j++) {
          if (toSkip.contains(j)) {
            continue;
          }
          var next = group.get(j);
          var nextWithOffset = withOffset(next, base.originTile(), next.originTile());
          if (shouldBeMerged(base, nextWithOffset)) {
            toUnify.add(nextWithOffset.polygon());
            toSkip.add(j);
          }
        }

        toUnify.add(base.polygon());
        var prettierToUnify = polygonPrettier.apply(toUnify);
        var unified = new UnifiedRoute(prettierToUnify, unionConf).unified();
        for (var p : unified) {
          newGroupedTiledPolygons.add(
              new TiledPolygon(p, base.type(), base.originTile(), base.tilingConf()));
        }
      }

      newTiledPolygons.add(newGroupedTiledPolygons);
    }
    return newTiledPolygons.stream().flatMap(Set::stream).collect(toSet());
  }

  private TiledPolygon withOffset(TiledPolygon p, IntXY originTile, IntXY currentTile) {
    var xFactor = currentTile.x() - originTile.x();
    var yFactor = currentTile.y() - originTile.y();
    var imgSize = tilingConf.imgSize();
    var coordinates = p.polygon().getCoordinates();
    if (!coordinates[0].equals(coordinates[coordinates.length - 1])) {
      var clone = new Coordinate[coordinates.length + 1];
      System.arraycopy(coordinates, 0, clone, 0, coordinates.length);
      clone[coordinates.length] = coordinates[0];
      coordinates = clone;
    }
    var polygon =
        geometryFactory.createPolygon(
            Arrays.stream(coordinates)
                .map(c -> new Coordinate(c.x + xFactor * imgSize, c.y + yFactor * imgSize))
                .toArray(Coordinate[]::new));
    return new TiledPolygon(polygon, p.type(), p.originTile(), p.tilingConf());
  }

  private boolean shouldBeMerged(TiledPolygon base, TiledPolygon other) {
    try {
      var basePolygon = base.polygon();
      var otherPolygon = other.polygon();
      return base.originTile().compareTo(other.originTile()) != 0
          && (basePolygon.distance(otherPolygon) < 10
              || isCollinearEnough(basePolygon, otherPolygon));
    } catch (Exception ignored) {
      return false;
    }
  }

  private TwoLineInt findTwoCloseLineInt(Set<LineInt> base, Set<LineInt> other) {
    TwoLineInt closest = new TwoLineInt(null, null);
    var distance = Double.MAX_VALUE;
    for (var baseLine : base) {
      for (var otherLine : other) {
        double dx1 = baseLine.b().x() - baseLine.a().x();
        double dy1 = baseLine.b().y() - baseLine.a().y();
        double dx2 = otherLine.b().x() - otherLine.a().x();
        double dy2 = otherLine.b().y() - otherLine.a().y();
        var currentDistance =
            Math.sqrt(Math.pow(Math.abs(dx1 - dx2), 2) + Math.pow(Math.abs(dy1 - dy2), 2));
        if (currentDistance < distance) {
          closest = new TwoLineInt(baseLine, otherLine);
          distance = currentDistance;
        }
      }
    }
    return closest;
  }

  public boolean isCollinearEnough(Polygon base, Polygon other) {
    Coordinate[] baseCoords = base.getExteriorRing().getCoordinates();
    Coordinate[] otherCoords = other.getExteriorRing().getCoordinates();

    // Find all vertical edges from both polygons
    Set<LineInt> baseVerticalEdges = findLinearEdges(baseCoords);
    Set<LineInt> otherVerticalEdges = findLinearEdges(otherCoords);

    if (baseVerticalEdges.isEmpty() || otherVerticalEdges.isEmpty()) {
      return false;
    }

    var closestLines = findTwoCloseLineInt(baseVerticalEdges, otherVerticalEdges);
    var baseLine = closestLines.first();
    var otherLine = closestLines.second();
    return areVectorsCollinear(baseLine, otherLine);
  }

  public Set<LineInt> findLinearEdges(Coordinate[] coords) {
    Set<LineInt> linearEdges = new HashSet<>();
    var minXDistance = mergeConf.minXDistance();
    var minYDistance = mergeConf.minYDistance();

    for (int i = 0; i < coords.length - 1; i++) {
      Coordinate c1 = coords[i];
      Coordinate c2 = coords[i + 1];

      // Check if segment is vertical (x coordinates are nearly equal)
      if (Math.abs(c1.x - c2.x) < minXDistance && Math.abs(c1.y - c2.y) > minYDistance) {
        linearEdges.add(new LineInt(new IntXY(c1), new IntXY(c2)));
      }
      if (Math.abs(c1.y - c2.y) < minXDistance && Math.abs(c1.x - c2.x) > minYDistance) {
        linearEdges.add(new LineInt(new IntXY(c1), new IntXY(c2)));
      }
    }
    return linearEdges;
  }

  public boolean areVectorsCollinear(LineInt baseLine, LineInt otherLine) {
    var tolerance = mergeConf.directionTolerance();
    double dx1 = baseLine.b().x() - baseLine.a().x();
    double dy1 = baseLine.b().y() - baseLine.a().y();
    double dx2 = otherLine.b().x() - otherLine.a().x();
    double dy2 = otherLine.b().y() - otherLine.a().y();

    // Check if vectors are parallel (cross product near zero)
    return Math.abs(dx1 * dy2 - dy1 * dx2) <= tolerance;
  }
}
