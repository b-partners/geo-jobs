package app.bpartners.geojobs.service.cityjson.texture;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector3D;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RasterInfoProjector {

  @SneakyThrows
  public List<Vector3D> project(List<Vector3D> coords, String sourceCrs, String targetCrs) {
    if (sourceCrs.equalsIgnoreCase(targetCrs)) {
      return coords;
    }

    CoordinateReferenceSystem sourceCRS = CRS.decode(sourceCrs, true);
    CoordinateReferenceSystem targetCRS = CRS.decode(targetCrs, true);

    MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

    List<Vector3D> projected = new ArrayList<>();
    for (Vector3D coord : coords) {
      Coordinate sourceCoord = new Coordinate(coord.getX(), coord.getY());
      Coordinate targetCoord = JTS.transform(sourceCoord, null, transform);
      projected.add(new Vector3D(targetCoord.getX(), targetCoord.getY(), coord.getZ()));
    }

    return projected;
  }
}
