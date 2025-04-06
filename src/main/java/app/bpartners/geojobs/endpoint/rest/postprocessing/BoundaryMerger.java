package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.route.UnifiedRoute;
import app.bpartners.geojobs.model.geometry.route.UnionConf;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

public class BoundaryMerger implements Function<File, List<LatLonPolygon>> {
    private final GeoJsonLoader geoJsonLoader;
    private final TilingConf tilingConf;
    private final UnionConf unionConf;
    private final NeighbourHoodHandler neighbourHoodHandler;
    private final ExecutorService executorService =
            newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() / 2));

    public BoundaryMerger(TilingConf tilingConf, UnionConf unionConf, int neighbourhoodTileDistance) {
        this.tilingConf = tilingConf;
        this.unionConf = unionConf;
        this.neighbourHoodHandler = new NeighbourHoodHandler(neighbourhoodTileDistance);
        this.geoJsonLoader = new GeoJsonLoader();
    }


    @Override
    public List<LatLonPolygon> apply(File file) {
        var latLonPolygons = geoJsonLoader.apply(file);
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
                .map(TiledPolygon::latLonPolygon).toList();
    }

    private Stream<TiledPolygon> futureStream(Future<Set<TiledPolygon>> future) {
        try {
            return future.get().stream();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<TiledPolygon> applyBoundaryMerge(Set<TiledPolygon> tiledPolygons) {
        var polygons = new ArrayList<>(tiledPolygons);
        var newTiledPolygons = new HashSet<TiledPolygon>();
        var size = tiledPolygons.size();
        for (int i = 0; i < size - 1; i++) {
            var currentPolygon = polygons.get(i);
            for (int j = i + 1; j < size; j++) {
                var nextPolygons = polygons.get(j).polygon();
                if (currentPolygon.polygon().intersects(nextPolygons)) {
                    var unified = new UnifiedRoute(Set.of(currentPolygon.polygon(), nextPolygons), unionConf).unified();
                    newTiledPolygons.add(new TiledPolygon(null, currentPolygon.type(), currentPolygon.originTile(), currentPolygon.tilingConf()));
                }
            }
        }
        return newTiledPolygons;
    }
}
