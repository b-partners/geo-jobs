package app.bpartners.geojobs.service.cityjson.texture.model;

import lombok.Builder;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

//TODO: remove unused fields
@Builder(toBuilder = true)
public record RasterInfo(
    // UNUSED FIELDS:
    double originX,
    double originY,
    double pixelWidth,
    double pixelHeight,
    double shearX,
    double shearY,
    CoordinateReferenceSystem crs,
    //USED FIELDS:
    int width,
    int height,
    int zoom,
    int tileX,
    int tileY,
    int tileImageSizePx
) {}
