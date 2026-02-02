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

  public boolean isSameDirection(Vector3D other, double degEpsilon) {
    double dot = this.dot(other);
    double norm2 = this.norm() * other.norm();

    if (norm2 == 0.0) return false;

    double absCos = Math.abs(dot / norm2);
    double minCos = Math.cos(Math.toRadians(degEpsilon));

    return absCos >= minCos;
  }

  public boolean isPerpendicular(Vector3D other, double degEpsilon) {
    double dot = this.dot(other);
    double norm2 = this.norm() * other.norm();

    if (norm2 == 0.0) return false;

    double absCos = Math.abs(dot / norm2);
    double maxCos = Math.cos(Math.toRadians(90.0 - degEpsilon));

    return absCos <= maxCos;
  }
}
