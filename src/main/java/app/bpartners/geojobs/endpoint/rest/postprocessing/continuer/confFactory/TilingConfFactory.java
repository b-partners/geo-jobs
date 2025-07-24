package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Envelope;

public class TilingConfFactory implements Function<Set<LatLonPolygon>, TilingConf> {

  @Override
  public TilingConf apply(Set<LatLonPolygon> polygons) {
    if (polygons == null || polygons.isEmpty()) {
      return TilingConf.getDefaultInstance();
    }

    Envelope envelope = new Envelope();
    for (LatLonPolygon poly : polygons) {
      envelope.expandToInclude(poly.polygon().getEnvelopeInternal());
    }

    double width = envelope.getWidth();
    double height = envelope.getHeight();
    double maxExtent = Math.max(width, height);

    int zoom = computeZoomLevel(maxExtent);
    int imgSize = 1024;

    return new TilingConf(zoom, imgSize);
  }

  private static int computeZoomLevel(double maxExtentDegrees) {
    for (int z = 0; z <= 20; z++) {
      double tileDegrees = 360.0 / Math.pow(2, z);
      if (tileDegrees <= maxExtentDegrees) {
        return Math.max(0, z - 1);
      }
    }
    return 20;
  }
}
