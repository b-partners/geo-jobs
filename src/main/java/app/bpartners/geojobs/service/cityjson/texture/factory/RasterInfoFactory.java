package app.bpartners.geojobs.service.cityjson.texture.factory;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONTexture;
import app.bpartners.geojobs.service.cityjson.texture.RasterInfoProjector;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import java.util.List;
import org.locationtech.jts.math.Vector3D;

public class RasterInfoFactory {
  public static RasterInfo create(RasterInfoProjector projector, CityJSONTexture texture) {
    // 1. Reproject origin (lon/lat → Lambert-93 meters)
    List<Vector3D> origin =
        projector.project(
            List.of(new Vector3D(texture.getTopLeftLongitude(), texture.getTopLeftLatitude(), 0)),
            "EPSG:4326",
            "EPSG:2154");

    Vector3D o = origin.get(0);

    return new RasterInfo(
        o.getX(),
        o.getY(),
        texture.getPixelWidth(),
        texture.getPixelHeight(),
        texture.getShearX(),
        texture.getShearY(),
        texture.getImageWidth(),
        texture.getImageHeight(),
        "EPSG:2154");
  }
}
