package app.bpartners.geojobs.model.lidar.api;

import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;

public interface LidarApi extends Function<Envelope, Set<String>> {}
