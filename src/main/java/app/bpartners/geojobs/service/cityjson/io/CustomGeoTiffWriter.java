package app.bpartners.geojobs.service.cityjson.io;

import static app.bpartners.geojobs.file.FileWriter.createTempFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.springframework.stereotype.Component;

@Component
public class CustomGeoTiffWriter {

  public File toGeoTiffFile(
      BufferedImage image, double originX, double originY, double pixelSizeMeters) {
    if (image.getWidth() <= 0 || image.getHeight() <= 0) {
      throw new IllegalArgumentException(
          "Image must have positive dimensions, got: "
              + image.getWidth()
              + "x"
              + image.getHeight());
    }
    if (pixelSizeMeters <= 0) {
      throw new IllegalArgumentException(
          "pixelSizeMeters must be positive, got: " + pixelSizeMeters);
    }

    try {
      double maxX = originX + image.getWidth() * pixelSizeMeters;
      double minY = originY - image.getHeight() * pixelSizeMeters;

      ReferencedEnvelope env =
          new ReferencedEnvelope(originX, maxX, minY, originY, DefaultGeographicCRS.WGS84);

      GridCoverage2D coverage = new GridCoverageFactory().create("texture", image, env);

      File out = createTempFile("geotiff-", ".tif");
      GeoTiffWriter writer = new GeoTiffWriter(out);
      try {
        writer.write(coverage, null);
      } finally {
        writer.dispose();
      }

      return out;

    } catch (IOException e) {
      throw new IllegalStateException("Fail to write GeoTiff file: " + e);
    }
  }
}
