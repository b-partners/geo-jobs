package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnifiedRoute;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriFunction;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class BoundaryMerger
    implements TriFunction<Set<TiledPolygon>, DetectableType, Double, Set<LatLonPolygon>> {
  private final TilingConf tilingConf;
  private final UnionConf unionConf;
  private final NeighbourHoodHandler neighbourHoodHandler;
  private final MergeConf mergeConf;
  private final PolygonPrettier prettier;
  private final ExecutorService executorService =
      newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() / 2));

  public BoundaryMerger(
      TilingConf tilingConf,
      UnionConf unionConf,
      MergeConf mergeConf,
      PrettyConf prettyConf,
      int neighbourhoodTileDistance) {
    this.tilingConf = tilingConf;
    this.unionConf = unionConf;
    this.neighbourHoodHandler = new NeighbourHoodHandler(neighbourhoodTileDistance);
    this.mergeConf = mergeConf;
    this.prettier = new PolygonPrettier(prettyConf);
  }

  public static TiledPolygon withOffset(TiledPolygon p, IntXY originTile, TilingConf tilingConf) {
    var currentTile = p.originTile();
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

  private Set<LatLonPolygon> merge(Set<TiledPolygon> polygons, double areaThreshold) {
    var origin = new ArrayList<>(polygons).getFirst().originTile();
    var tiledPolygonsWithOffset =
        polygons.stream().map(tile -> withOffset(tile, origin, tilingConf)).toList();
    var result = new HashSet<TiledPolygon>();
    var alreadyUnified = new HashSet<Polygon>();
    for (var tp : tiledPolygonsWithOffset) {
      if (alreadyUnified.contains(tp.polygon())) {
        continue;
      }
      var aroundPolygons =
          tiledPolygonsWithOffset.stream()
              .filter(p -> smallestAround(tp, p, areaThreshold))
              .collect(toSet());
      if (!aroundPolygons.isEmpty()) {
        var smallest = new ArrayList<>(aroundPolygons.stream().map(TiledPolygon::polygon).toList());
        smallest.sort(Comparator.comparing(Polygon::getArea));
        var toUnify = smallest.getFirst();
        alreadyUnified.addAll(Set.of(toUnify, tp.polygon()));
        var unified = new UnifiedRoute(Set.of(toUnify, tp.polygon()), unionConf).unified();
        for (var p : unified) {
          var convexHull = p.convexHull();
          var md = new MinimumDiameter(convexHull);
          result.add(
              new TiledPolygon(
                  (Polygon) md.getMinimumRectangle(), tp.type(), tp.originTile(), tp.tilingConf()));
        }
      } else {
        result.add(tp);
      }
    }
    log.info("Already unified polygons: {}", alreadyUnified.size());
    return result.stream().map(tp -> tp.latLonPolygon(origin)).collect(toSet());
  }

  private boolean smallestAround(TiledPolygon ref, TiledPolygon other, double areaThreshold) {
    var origin = ref.originTile();
    var tile = other.originTile();

    if (other.polygon().getArea() > areaThreshold) {
      return false;
    }

    if (ref.originTile().equals(other.originTile())) {
      return false;
    }
    int dx = Math.abs(tile.x() - origin.x());
    int dy = Math.abs(tile.y() - origin.y());
    var refBoundary = ref.polygon().getBoundary().buffer(10);
    var otherBoundary = other.polygon().getBoundary().buffer(10);
    var intersection = refBoundary.intersection(otherBoundary);
    return (2 > dx && dy < 2) && intersection.getLength() > 100;
  }

  private Set<LatLonPolygon> parallelMerge(Set<TiledPolygon> tiledPolygons) {
    var origin = new ArrayList<>(tiledPolygons).getFirst().originTile();
    // TODO: handle side effect on concurrent execution
    /**
     * var polygonsByNeighbourhood = neighbourHoodHandler.aroundN(tiledPolygons);
     * List<Future<Set<LatLonPolygon>>> futures; try { futures = executorService.invokeAll(
     * polygonsByNeighbourhood.stream().map(neighbourhoodPolygons -> ((Callable<Set<LatLonPolygon>>)
     * () -> merge(neighbourhoodPolygons, origin))).collect(toSet())); } catch (InterruptedException
     * e) { throw new RuntimeException(e); } return
     * futures.stream().flatMap(this::futureStream).collect(toSet());*
     */
    return merge(tiledPolygons, origin);
  }

  private Stream<LatLonPolygon> futureStream(Future<Set<LatLonPolygon>> future) {
    try {
      return future.get().stream();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<LatLonPolygon> merge(Set<TiledPolygon> polygons, IntXY origin) {
    var tiledPolygonsWithOffset =
        polygons.stream().map(tile -> withOffset(tile, origin, tilingConf)).toList();

    var result = new HashSet<TiledPolygon>();
    var alreadyUnified = new HashSet<Polygon>();
    var progress = 0;

    for (var tp : tiledPolygonsWithOffset) {
      if (isAlreadyProcessed(tp.polygon(), alreadyUnified)) {
        continue;
      }

      var aroundPolygons =
          tiledPolygonsWithOffset.stream()
              .filter(p -> !p.equals(tp) && shouldBeMerged(tp, p))
              .collect(Collectors.toSet());

      var toUnify = aroundPolygons.stream().map(TiledPolygon::polygon).collect(Collectors.toSet());
      toUnify.add(tp.polygon());

      alreadyUnified.addAll(toUnify);

      var prettyPolygons = prettier.apply(toUnify);

      var unified = new UnifiedRoute(prettyPolygons, unionConf).unified();

      for (var p : unified) {
        result.add(new TiledPolygon(p, tp.type(), tp.originTile(), tp.tilingConf()));
      }

      log.info("progression: {}/{}", ++progress, tiledPolygonsWithOffset.size());
    }

    return result.stream().map(tp -> tp.latLonPolygon(origin)).collect(Collectors.toSet());
  }

  private boolean isAlreadyProcessed(Polygon candidate, Set<Polygon> alreadyProcessed) {
    return alreadyProcessed.stream().anyMatch(p -> p.equalsTopo(candidate));
  }

  private boolean shouldBeMerged(TiledPolygon base, TiledPolygon other) {
    try {
      var baseTile = base.originTile();
      var otherTile = other.originTile();

      if (baseTile.equals(otherTile)) return false;

      int dx = Math.abs(otherTile.x() - baseTile.x());
      int dy = Math.abs(otherTile.y() - baseTile.y());
      if (dx > 1 || dy > 1) return false;

      var basePolygon = base.polygon();
      var otherPolygon = other.polygon();

      if (basePolygon.distance(otherPolygon) > 50) return false;

      var refBoundary = basePolygon.getBoundary().buffer(50);
      var otherBoundary = otherPolygon.getBoundary().buffer(50);
      var intersection = refBoundary.intersection(otherBoundary);
      return intersection.getLength() > 200;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Set<LatLonPolygon> apply(
      Set<TiledPolygon> tiledPolygons, DetectableType detectableType, Double areaThreshold) {
    return switch (detectableType) {
      case TOMBE, PASSAGE_PIETON, PISCINE -> merge(tiledPolygons, areaThreshold);
      default -> parallelMerge(tiledPolygons);
    };
  }
}
