package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import lombok.Getter;

public final class ParallelTiledLinesContinuer extends LinesContinuer<TiledPolygon> {
  @Getter private final TiledLinesContinuer tiledLinesContinuer;

  private final int neighbourhoodTileDistance;
  // e.g.: if equals 10, then will have 10*10=100 tiles in neighbourhood

  private final ExecutorService executorService =
      newFixedThreadPool(getRuntime().availableProcessors());

  public ParallelTiledLinesContinuer(
      RoutesContinuationConf continuationConf,
      TilingConf tilingConf,
      int neighbourhoodTileDistance) {
    this.tiledLinesContinuer = new TiledLinesContinuer(continuationConf, tilingConf);
    this.neighbourhoodTileDistance = neighbourhoodTileDistance;
  }

  @Override
  public Set<TiledPolygon> apply(Set<TiledPolygon> polygons) {
    Map<IntXY, Set<TiledPolygon>> polygonsByNeighbourhood = polygonsByNeighbourhood(polygons);

    List<Future<Set<TiledPolygon>>> futures;
    try {
      futures =
          executorService.invokeAll(
              polygonsByNeighbourhood.values().stream()
                  .map(
                      neighbourhoodPolygons ->
                          ((Callable<Set<TiledPolygon>>)
                              () -> tiledLinesContinuer.apply(neighbourhoodPolygons)))
                  .collect(toSet()));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return futures.stream().flatMap(this::futureStream).collect(toSet());
  }

  private Stream<TiledPolygon> futureStream(Future<Set<TiledPolygon>> future) {
    try {
      return future.get().stream();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<IntXY, Set<TiledPolygon>> polygonsByNeighbourhood(Set<TiledPolygon> polygons) {
    Map<IntXY, Set<TiledPolygon>> res = new HashMap<>();

    for (var p : polygons) {
      var originTile = p.originTile();
      var neighbourhood =
          new IntXY(
              originTile.x() / neighbourhoodTileDistance,
              originTile.y() / neighbourhoodTileDistance);
      res.putIfAbsent(neighbourhood, new HashSet<>());
      res.get(neighbourhood).add(p);
    }

    return res;
  }
}
