package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import java.util.function.Function;

public interface ObjectsDetector extends Function<DetectionTask, DetectionResponse> {}
