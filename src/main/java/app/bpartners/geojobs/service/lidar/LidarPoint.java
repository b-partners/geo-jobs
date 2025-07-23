package app.bpartners.geojobs.service.lidar;

import com.github.mreutegg.laszip4j.LASPoint;

public record LidarPoint(LASPoint lasPoint, int label) {
}
