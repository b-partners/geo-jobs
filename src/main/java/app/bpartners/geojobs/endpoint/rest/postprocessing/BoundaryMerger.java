package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.route.UnifiedRoute;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.io.File;
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

public class BoundaryMerger implements Function<Set<LatLonPolygon>, Set<LatLonPolygon>> {
    private final TilingConf tilingConf;
    private final UnionConf unionConf;
    private final NeighbourHoodHandler neighbourHoodHandler;
    private final ExecutorService executorService =
            newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() / 2));

    public BoundaryMerger(TilingConf tilingConf, UnionConf unionConf, int neighbourhoodTileDistance) {
        this.tilingConf = tilingConf;
        this.unionConf = unionConf;
        this.neighbourHoodHandler = new NeighbourHoodHandler(neighbourhoodTileDistance);
    }


    @Override
    public Set<LatLonPolygon> apply(Set<LatLonPolygon> latLonPolygons) {
        var tiledPolygons = latLonPolygons.stream().map(p -> p.tiledPolygon(tilingConf)).collect(toSet());
        Map<IntXY, Set<TiledPolygon>> polygonsByNeighbourhood = neighbourHoodHandler.apply(tiledPolygons);
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
            var toSkip = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                var base = group.get(i);
                var toUnify = new HashSet<Polygon>();

                if (toSkip.contains(i)) {
                    continue;
                }

                for (int j = i + 1; j < size ; j++) {
                    var next = group.get(j);
                    var nextWithOffset = withOffset(next, base.originTile(), next.originTile());
                    if (isClosedEnoughBuNotInTheSameTile(base, nextWithOffset)) {
                        toUnify.add(nextWithOffset.polygon());
                        toSkip.add(j);
                    }
                }

                toUnify.add(base.polygon());
                var unified = new UnifiedRoute(toUnify, unionConf).unified();

                for (Polygon p : unified) {
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

    private boolean isClosedEnoughBuNotInTheSameTile(TiledPolygon base, TiledPolygon next) {
        try {
            return base.polygon().intersects(next.polygon()) && !base.originTile().equals(next.originTile());
        }catch (Exception e) {
            return false;
        }
    }
}
