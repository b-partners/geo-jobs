package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.model.geometry.IntXY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NeighbourHoodHandler implements Function<Set<TiledPolygon>, Map<IntXY, Set<TiledPolygon>>> {
    private final int neighbourhoodTileDistance;
    // e.g.: if equals 10, then will have 10*10=100 tiles in neighbourhood

    public NeighbourHoodHandler(int neighbourhoodTileDistance) {
        this.neighbourhoodTileDistance = neighbourhoodTileDistance;
    }

    @Override
    public Map<IntXY, Set<TiledPolygon>> apply(Set<TiledPolygon> polygons) {
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
