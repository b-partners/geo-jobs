package app.bpartners.geojobs.service;


import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.service.tiling.TileFinder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileFinderTest {
    TileFinder subject = new TileFinder();

    @Test
    void get_surrounding_tiles() {
        var latitude = BigDecimal.valueOf(46.651930);
        var longitude = BigDecimal.valueOf(-0.249317);
        var zoom = 20;
        var centralPoint = new TileCoordinates().x(523561).y(370293).z(zoom);

        var actual = subject.getSurroundingTiles(longitude, latitude, zoom);

        assertEquals(9, actual.size());
        assertEquals(centralPoint, actual.get(4));
    }
}
