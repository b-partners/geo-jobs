package app.bpartners.geojobs.service.cityjson.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class CustomGeoTiffWriter {

  public File toGeoTiffFile(
      BufferedImage image, double originX, double originY, double pixelSizeMeters) {
    try {
      double maxX = originX + image.getWidth() * pixelSizeMeters;
      double minY = originY - image.getHeight() * pixelSizeMeters;

      ReferencedEnvelope env =
          new ReferencedEnvelope(originX, maxX, minY, originY, DefaultGeographicCRS.WGS84);

      GridCoverage2D coverage = new GridCoverageFactory().create("texture", image, env);

      File out = tempFile();
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

  private File tempFile() {
    try {
      return Files.createTempFile("geotiff-", ".tif").toFile();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file: " + e);
    }
  }
}
