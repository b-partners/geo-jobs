package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import java.util.List;
import java.util.function.BiFunction;

public interface ObjectsDetector
    extends BiFunction<DetectionTask, List<DetectableType>, DetectionResponse> {}
