package app.bpartners.geojobs.service;

import java.util.function.Function;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

@Component
public class GeometrySquareMeterArea implements Function<Geometry, Double> {

  public static final CoordinateReferenceSystem WGS84;
  public static final CoordinateReferenceSystem LAMBERT_93;

  static {
    try {
      WGS84 = CRS.decode("EPSG:4326", true);
      LAMBERT_93 = CRS.decode("EPSG:2154", true);
    } catch (Exception e) {
      throw new RuntimeException("Erreur chargement des CRS", e);
    }
  }

  @Override
  public Double apply(Geometry geometry) {
    try {
      var transform = CRS.findMathTransform(WGS84, LAMBERT_93, true);
      Geometry projected = JTS.transform(geometry, transform);
      return projected.getArea(); // en m²
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
