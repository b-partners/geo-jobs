package app.bpartners.geojobs.service.lidar;

import app.bpartners.geojobs.repository.model.detection.DetectableType;

public class LidarClassMapper {
    public DetectableType toDomain(int classIndex){
        return switch (classIndex){
            case 2 -> DetectableType.BACKGROUND;
            case 6 -> DetectableType.BATI_ARDOISE;
            default -> throw new IllegalStateException("Unexpected value: " + classIndex);
        };
    }
}
