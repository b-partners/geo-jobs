package app.bpartners.geojobs.service.cityjson.texture.model;

import java.io.File;

public record TextureFile(String dataUri, RasterInfo rasterInfo, File tifFile) {}
