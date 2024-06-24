package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import java.util.function.BiFunction;

public interface TileObjectDetector
    extends BiFunction<TileDetectionTask, List<DetectableObjectConfiguration>, DetectionResponse> {}
