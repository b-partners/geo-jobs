package app.bpartners.geojobs.service.lidar;

import com.github.mreutegg.laszip4j.LASPoint;

import static java.lang.Float.compare;

public record Dimension(Roof roof, Sol sol) {
    private static double round2(double value) {
        return Math.ceil(value * 100) / 100.0;
    }

    public double getSlopeInDegrees() {
        var minZPoint = roof.points().stream()
                .map(LidarPoint::lasPoint)
                .min((a, b) -> compare(a.getZ(), b.getZ())).orElseThrow();
        var maxZPoint = roof.points().stream()
                .map(LidarPoint::lasPoint)
                .max((a, b) -> compare(a.getZ(), b.getZ())).orElseThrow();

        double dx = maxZPoint.getX() - minZPoint.getX();
        double dy = maxZPoint.getY() - minZPoint.getY();
        double dz = maxZPoint.getZ() - minZPoint.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            return round2(Math.toDegrees(Math.atan(dz / distance)));
        }

        return 0;
    }

    public double getHeightInMeters() {
        var minZPoint = roof.points().stream()
                .map(LidarPoint::lasPoint)
                .min((a, b) -> compare(a.getZ(), b.getZ())).orElseThrow();

        double meanSolZ = sol.points().stream()
                .map(LidarPoint::lasPoint)
                .mapToDouble(LASPoint::getZ)
                .average().orElseThrow();

        return round2(minZPoint.getZ() - meanSolZ);
    }
}
