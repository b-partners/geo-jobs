package app.bpartners.geojobs.service.lidar.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

@Getter
@EqualsAndHashCode(callSuper = false)
public class LasPointGeometry extends Point {
  private final LASHeader lasHeader;
  private final LASPoint lasPoint;

  public LasPointGeometry(LASPoint lasPoint, LASHeader lasHeader) {
    super(lasPointToSequence(lasPoint, lasHeader), geometryFactory);

    this.lasHeader = lasHeader;
    this.lasPoint = lasPoint;
  }

  private static CoordinateSequence lasPointToSequence(LASPoint point, LASHeader header) {
    double x = point.getX() * header.getXScaleFactor() + header.getXOffset();
    double y = point.getY() * header.getYScaleFactor() + header.getYOffset();
    double z = point.getZ() * header.getZScaleFactor() + header.getZOffset();

    return new CoordinateArraySequence(new Coordinate[] {new Coordinate(x, y, z)});
  }
}
