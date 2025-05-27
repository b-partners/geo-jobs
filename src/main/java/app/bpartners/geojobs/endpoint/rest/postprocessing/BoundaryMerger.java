package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
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
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class BoundaryMerger
    implements BiFunction<Set<TiledPolygon>, DetectableType, Set<LatLonPolygon>> {
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

  private Set<LatLonPolygon> merge(Set<TiledPolygon> polygons) {
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
          tiledPolygonsWithOffset.stream().filter(p -> smallestAround(tp, p)).collect(toSet());
      if (!aroundPolygons.isEmpty()) {
        var smallest = new ArrayList<>(aroundPolygons.stream().map(TiledPolygon::polygon).toList());
        smallest.sort(Comparator.comparing(Polygon::getArea));
        var toUnify = smallest.getFirst();
        alreadyUnified.addAll(Set.of(toUnify, tp.polygon()));
        var unified = new UnifiedRoute(Set.of(toUnify, tp.polygon()), unionConf).unified();
        for (var p : unified) {
          result.add(
              new TiledPolygon(
                  (Polygon) p.getEnvelope(), tp.type(), tp.originTile(), tp.tilingConf()));
        }
      } else {
        result.add(tp);
      }
    }
    log.info("Already unified polygons: {}", alreadyUnified.size());
    return result.stream().map(tp -> tp.latLonPolygon(origin)).collect(toSet());
  }

  private boolean smallestAround(TiledPolygon ref, TiledPolygon other) {
    var origin = ref.originTile();
    var tile = other.originTile();
    if (other.polygon().getArea() > 4000) {
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
    return (-1 <= dx && dy <= 1) && intersection.getLength() > 100;
  }

  private Set<LatLonPolygon> parallelMerge(Set<TiledPolygon> tiledPolygons) {
    var origin = new ArrayList<>(tiledPolygons).getFirst().originTile();
    // TODO: handle side effect on concurrent execution
    /**
     * var polygonsByNeighbourhood = neighbourHoodHandler.aroundN(tiledPolygons);
     * List<Future<Set<LatLonPolygon>>> futures; try { futures = executorService.invokeAll(
     * polygonsByNeighbourhood.stream().map(neighbourhoodPolygons -> ((Callable<Set<LatLonPolygon>>)
     * () -> merge(neighbourhoodPolygons, origin))).collect(toSet())); } catch
     * (InterruptedException e) { throw new RuntimeException(e); } return
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
    for (var tp : tiledPolygonsWithOffset) {
      if (alreadyUnified.contains(tp.polygon())) {
        continue;
      }
      var aroundPolygons =
          tiledPolygonsWithOffset.stream().filter(p -> shouldBeMerged(tp, p)).collect(toSet());
      if (!aroundPolygons.isEmpty()) {
        var toUnify = aroundPolygons.stream().map(TiledPolygon::polygon).collect(toSet());
        toUnify.add(tp.polygon());
        alreadyUnified.addAll(toUnify);
        var prettyPolygons = prettier.apply(toUnify);
        var unified = new UnifiedRoute(prettyPolygons, unionConf).unified();
        for (var p : unified) {
          result.add(new TiledPolygon(p, tp.type(), tp.originTile(), tp.tilingConf()));
        }
      } else {
        result.add(tp);
      }
    }
    return result.stream().map(tp -> tp.latLonPolygon(origin)).collect(toSet());
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

  @Override
  public Set<LatLonPolygon> apply(Set<TiledPolygon> tiledPolygons, DetectableType detectableType) {
    return switch (detectableType) {
      case TOMBE, PASSAGE_PIETON, PISCINE -> merge(tiledPolygons);
      default -> parallelMerge(tiledPolygons);
    };
  }
}
