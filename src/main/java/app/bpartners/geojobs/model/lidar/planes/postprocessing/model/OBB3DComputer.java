package app.bpartners.geojobs.model.lidar.planes.postprocessing.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class OBB3DComputer implements Function<Plane3D, Polygon> {
  private final OBB2DComputer obb2DComputer;

  public OBB3DComputer() {
    this(new OBB2DComputer());
  }

  @Override
  public Polygon apply(Plane3D plane) {
    var obb2D = obb2DComputer.apply(plane);

    var center = obb2D.center();
    double halfW = obb2D.width() / 2.0;
    double halfH = obb2D.height() / 2.0;
    double angle = obb2D.angle();

    double cos = Math.cos(angle);
    double sin = Math.sin(angle);

    double[][] corners2D =
        new double[][] {{-halfW, -halfH}, {halfW, -halfH}, {halfW, halfH}, {-halfW, halfH}};

    var coordinates3D = new Coordinate[5];
    for (int i = 0; i < 4; i++) {
      double x2d = corners2D[i][0];
      double y2d = corners2D[i][1];

      double x = center.getX() + x2d * cos - y2d * sin;
      double y = center.getY() + x2d * sin + y2d * cos;

      double z;
      double a = plane.getA();
      double b = plane.getB();
      double c = plane.getC();
      double d = plane.getD();
      if (Math.abs(c) > 1e-12) {
        z = (-d - a * x - b * y) / c;
      } else {
        z = center.getZ();
      }

      coordinates3D[i] = new Coordinate(x, y, z);
    }
    coordinates3D[4] = coordinates3D[0];

    return geometryFactory.createPolygon(coordinates3D);
  }
}
