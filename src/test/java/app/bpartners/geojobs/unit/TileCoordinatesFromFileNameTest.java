package app.bpartners.geojobs.unit;

import app.bpartners.geojobs.model.geometry.TileCoordinatesFromFileName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TileCoordinatesFromFileNameTest {
    TileCoordinatesFromFileName subject;
    @Test
    void is_x_y_z_dot_filetype_ok() {
        subject = new TileCoordinatesFromFileName(true);
        var filename = "20_529770_351292.jpg";

        assertEquals(20, subject.z(filename));
        assertEquals(529770, subject.x(filename));
        assertEquals(351292, subject.y(filename));
    }

    @Test
    void is_x_y_z_dot_not_filetype_ok() {
        subject = new TileCoordinatesFromFileName(false);
        var filename = "test_20_529770_351292.jpg";

        assertEquals(20, subject.z(filename));
        assertEquals(529770, subject.x(filename));
        assertEquals(351292, subject.y(filename));
    }
}
