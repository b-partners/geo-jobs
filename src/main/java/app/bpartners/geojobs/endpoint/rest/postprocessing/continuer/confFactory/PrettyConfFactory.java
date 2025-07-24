package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import java.util.Set;
import java.util.function.Function;

public class PrettyConfFactory implements Function<Set<LatLonPolygon>, PrettyConf> {

  @Override
  public PrettyConf apply(Set<LatLonPolygon> polygons) {
    if (polygons == null || polygons.isEmpty()) return new PrettyConf(10d);

    double avgEdgeLength =
        polygons.stream().mapToDouble(PrettyConfFactory::averageEdgeLength).average().orElse(20d);

    double maxDiagonal =
        polygons.stream()
            .map(p -> p.polygon().getEnvelopeInternal())
            .mapToDouble(env -> Math.hypot(env.getWidth(), env.getHeight()))
            .max()
            .orElse(100d);

    double base = avgEdgeLength * 0.75d;
    double scaled = Math.max(base, maxDiagonal * 0.03d);
    double dpbThreshold = clamp(scaled);

    return new PrettyConf(dpbThreshold);
  }

  private static double averageEdgeLength(LatLonPolygon p) {
    var coords = p.polygon().getCoordinates();
    if (coords.length < 2) return 0d;

    double sum = 0d;
    for (int i = 0; i < coords.length - 1; i++) {
      sum += coords[i].distance(coords[i + 1]);
    }
    return sum / (coords.length - 1);
  }

  private static double clamp(double value) {
    return Math.max(5d, Math.min(150d, value));
  }
}
