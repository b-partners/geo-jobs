package app.bpartners.geojobs.service.cityjson.model;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.model.ChimneyPlane3D;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Lod {
  LOD_2("2"),
  LOD_3("3");

  @Getter private final String value;

  public static Lod from(Plane3D plane) {
    if (plane instanceof ChimneyPlane3D) {
      return LOD_3;
    }

    return LOD_2;
  }
}
