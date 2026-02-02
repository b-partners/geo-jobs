package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;

public record Vector3D(double x, double y, double z) {
  public static Vector3D from(LasPointGeometry a, LasPointGeometry b) {
    var subtracted = b.subtract(a);
    return new Vector3D(subtracted.getX(), subtracted.getY(), subtracted.getZ());
  }

  public double dot(Vector3D other) {
    return x * other.x + y * other.y + z * other.z;
  }

  public double norm() {
    return Math.sqrt(dot(this));
  }
}
