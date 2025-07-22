package app.bpartners.geojobs.model.geometry.polygon;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static org.geotools.referencing.CRS.findMathTransform;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;

import java.util.function.Function;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class PolygonReprojection implements Function<Geometry, Geometry> {
  public static final String EPSG_4326 = "EPSG:4326";
  public static final String EPSG_2154 = "EPSG:2154";

  private final CoordinateReferenceSystem sourceCRS;
  private final CoordinateReferenceSystem targetCRS;

  public PolygonReprojection(String sourceCRS, String targetCRS) {
    try {
      this.sourceCRS = CRS.decode(sourceCRS);
      this.targetCRS = CRS.decode(targetCRS);
    } catch (FactoryException e) {
      throw new RuntimeException(
          String.format(
              "Failed to create CoordinateReferenceSystem from source='%s' to target='%s",
              sourceCRS, targetCRS));
    }
  }

  @Override
  public Geometry apply(Geometry polygon) {
    var source = sourceCRS;
    var target = targetCRS;
    try {
      var transform = findMathTransform(source, target, true);
      return JTS.transform(polygon, transform);
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException(
          "Failed to reproject polygon from CRS '"
              + source
              + "' to '"
              + target
              + "'.",
          e);
    }
  }
}
