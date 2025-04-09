package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.LineInt;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnifiedRoute;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class BoundaryMerger implements Function<Set<LatLonPolygon>, Set<LatLonPolygon>> {
    private final TilingConf tilingConf;
    private final UnionConf unionConf;
    private final NeighbourHoodHandler neighbourHoodHandler;
    private final PolygonPrettier polygonPrettier;
    private final MergeConf mergeConf;
    private final ExecutorService executorService =
            newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() / 2));

    public BoundaryMerger(TilingConf tilingConf, UnionConf unionConf, PrettyConf prettyConf, MergeConf mergeConf, int neighbourhoodTileDistance) {
        this.tilingConf = tilingConf;
        this.unionConf = unionConf;
        this.neighbourHoodHandler = new NeighbourHoodHandler(neighbourhoodTileDistance);
        this.polygonPrettier = new PolygonPrettier(prettyConf);
        this.mergeConf = mergeConf;
    }


    @Override
    public Set<LatLonPolygon> apply(Set<LatLonPolygon> latLonPolygons) {
        var tiledPolygons = latLonPolygons.stream()
                .map(p -> p.tiledPolygon(tilingConf)).collect(toSet());
        var polygonsByNeighbourhood = neighbourHoodHandler.apply(tiledPolygons);
        List<Future<Set<TiledPolygon>>> futures;
        try {
            futures =
                    executorService.invokeAll(
                            polygonsByNeighbourhood.values().stream()
                                    .map(
                                            neighbourhoodPolygons ->
                                                    ((Callable<Set<TiledPolygon>>)
                                                            () -> applyBoundaryMerge(neighbourhoodPolygons)))
                                    .collect(toSet()));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return futures.stream()
                .flatMap(this::futureStream).collect(toSet()).stream()
                .map(TiledPolygon::latLonPolygon)
                .collect(toSet());
    }

    public Set<LatLonPolygon> from(Set<TiledPolygon> tiledPolygons){
        var latLonPolygons = tiledPolygons.stream()
                .map(TiledPolygon::latLonPolygon)
                .collect(toSet());
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
                log.info("merge progression {}/{}", i, size);
                var base = group.get(i);
                var toUnify = new HashSet<Polygon>();

                if (toSkip.contains(i)) {
                    continue;
                }

                for (int j = i + 1; j < size ; j++) {
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
                    newGroupedTiledPolygons.add(new TiledPolygon(p, base.type(), base.originTile(), base.tilingConf()));
                }
            }

            newTiledPolygons.add(newGroupedTiledPolygons);
        }
        return newTiledPolygons.stream()
                .flatMap(Set::stream)
                .collect(toSet());
    }

    private TiledPolygon withOffset(TiledPolygon p, IntXY originTile, IntXY currentTile) {
        var xFactor = currentTile.x() - originTile.x();
        var yFactor = currentTile.y() - originTile.y();
        var imgSize = tilingConf.imgSize();
        var polygon =  geometryFactory.createPolygon(
                Arrays.stream(p.polygon().getCoordinates())
                        .map(c -> new Coordinate(c.x + xFactor * imgSize, c.y + yFactor * imgSize))
                        .toArray(Coordinate[]::new));
        return new TiledPolygon(polygon, p.type(), p.originTile(), p.tilingConf());
    }

    private boolean shouldBeMerged(TiledPolygon base, TiledPolygon other) {
        try {
            return isCollinearEnough(base.polygon(), other.polygon())
                    && base.originTile().compareTo(other.originTile()) != 0;
        } catch (Exception ignored) {
            return false;
        }
    };

    public boolean isCollinearEnough(Polygon base, Polygon other) {
        Coordinate[] baseCoords = base.getExteriorRing().getCoordinates();
        Coordinate[] otherCoords = other.getExteriorRing().getCoordinates();

        // Find all vertical edges from both polygons
        List<Coordinate[]> baseVerticalEdges = findLinearEdges(baseCoords);
        List<Coordinate[]> otherVerticalEdges = findLinearEdges(otherCoords);

        if (baseVerticalEdges.isEmpty() || otherVerticalEdges.isEmpty()) {
            return false;
        }

        // Compare each vertical edge from base with each from other polygon
        for (Coordinate[] baseEdge : baseVerticalEdges) {
            var basePoint1 = new IntXY((int) baseEdge[0].x, (int) baseEdge[0].y);
            var basePoint2 = new IntXY((int) baseEdge[1].x, (int) baseEdge[1].y);
            var baseLine = new LineInt(basePoint1, basePoint2);
            for (Coordinate[] otherEdge : otherVerticalEdges) {
                var otherPoint1 = new IntXY((int) otherEdge[0].x, (int) otherEdge[0].y);
                var otherPoint2 = new IntXY((int) otherEdge[1].x, (int) otherEdge[1].y);
                var otherLine = new LineInt(otherPoint1, otherPoint2);

                if (areVectorsCollinear(baseLine, otherLine)) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<Coordinate[]> findLinearEdges(Coordinate[] coords) {
        List<Coordinate[]> linearEdges = new ArrayList<>();
        var minXDistance = mergeConf.minXDistance();
        var minYDistance = mergeConf.minYDistance();

        for (int i = 0; i < coords.length - 1; i++) {
            Coordinate c1 = coords[i];
            Coordinate c2 = coords[i + 1];

            // Check if segment is vertical (x coordinates are nearly equal)
            if (Math.abs(c1.x - c2.x) < minXDistance && Math.abs(c1.y - c2.y) > minYDistance) {
                linearEdges.add(new Coordinate[]{c1, c2});
            }
            if (Math.abs(c1.y - c2.y) < minXDistance && Math.abs(c1.x - c2.x) > minYDistance) {
                linearEdges.add(new Coordinate[]{c1, c2});
            }
        }
        return linearEdges;
    }


    private boolean areVectorsCollinear(LineInt baseLine, LineInt otherLine) {
        var tolerance = mergeConf.directionTolerance();
        double dx1 = otherLine.a().x() - baseLine.a().x();
        double dy1 = otherLine.a().y() - baseLine.a().y();
        double dx2 = otherLine.b().x() - baseLine.b().x();
        double dy2 = otherLine.b().y() - baseLine.b().y();

        // Normalize vectors to avoid magnitude differences
        double len1 = Math.hypot(dx1, dy1);
        double len2 = Math.hypot(dx2, dy2);

        if (len1 < tolerance || len2 < tolerance) {
            return false; // zero-length vector
        }

        dx1 /= len1; dy1 /= len1;
        dx2 /= len2; dy2 /= len2;

        // Check if vectors are parallel (cross product near zero)
        return Math.abs(dx1 * dy2 - dy1 * dx2) < tolerance;
    }


}
