package app.bpartners.geojobs.model.geometry.polygon;

import static org.geotools.referencing.CRS.findMathTransform;

import java.util.function.Function;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Polygon;

public class PolygonReprojection implements Function<Polygon, Polygon> {
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
  public Polygon apply(Polygon polygon) {
    try {
      var transform = findMathTransform(sourceCRS, targetCRS, true);
      return (Polygon) JTS.transform(polygon, transform);
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException(
          "Failed to reproject polygon from CRS '"
              + sourceCRS.getName()
              + "' to '"
              + targetCRS.getName()
              + "'.",
          e);
    }
  }
}
