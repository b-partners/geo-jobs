package app.bpartners.geojobs.service.lidar;

import app.bpartners.geojobs.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Set;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

public class LidarPolygonMetricProcessorIT extends FacadeIT {
    @Autowired LidarPolygonMetricProcessor subject;

    @Test
    void compute_roof_slope(){
        var lidarTile1 = new File(getClass().getResource("/las/LHD_FXX_0451_6622_PTS_C_LAMB93_IGN69.copc.laz").getFile());
        var lidarTile2 = new File(getClass().getResource("/las/LHD_FXX_0451_6623_PTS_C_LAMB93_IGN69.copc.laz").getFile());

        Coordinate[] coordinates = new Coordinate[] {
            new Coordinate(451566.8828060706, 6622000.718291294),
            new Coordinate(451560.0146623639, 6621980.508744332),
            new Coordinate(451610.4477301412, 6621973.113031218),
            new Coordinate(451612.2254675607, 6621992.92179322),
            new Coordinate(451566.8828060706, 6622000.718291294)
        };

        var roofGeometry = geometryFactory.createPolygon(coordinates);
        var actual = subject.apply(roofGeometry, Set.of(lidarTile1, lidarTile2));

        System.out.println("Height=" + actual.getHeightInMeters());
        System.out.println("Pente=" + actual.getSlopeInDegrees());
    }
}
