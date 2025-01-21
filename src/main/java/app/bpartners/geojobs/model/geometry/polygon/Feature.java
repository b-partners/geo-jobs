package app.bpartners.geojobs.model.geometry.polygon;

import static java.lang.Integer.parseInt;
import static lombok.AccessLevel.PRIVATE;

import app.bpartners.geojobs.model.geometry.IntXY;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = PRIVATE)
@Builder(toBuilder = true)
public class Feature {
  private final String filename;
  private final String label;
  private final double confidence;
  private final Polygon geometry;

  private final IntXY imageResolution;
  private static final Pattern filenamePattern =
      Pattern.compile("(\\w+)_(\\w+)_(\\w+)_(\\w+)\\.(\\w+)");
  private final int zoom;
  private final IntXY tileCoordinate;

  public Feature(
      String filename, String label, double confidence, Polygon geometry, IntXY imageResolution) {
    this.filename = filename;
    this.label = label;
    this.confidence = confidence;
    this.geometry = geometry;
    this.imageResolution = imageResolution;
    this.zoom = intFromFilename(filename, 2);
    this.tileCoordinate = new IntXY(intFromFilename(filename, 3), intFromFilename(filename, 4));
  }

  public Feature(String filename, String label, double confidence, Polygon geometry) {
    this.filename = filename;
    this.label = label;
    this.confidence = confidence;
    this.geometry = geometry;
    this.imageResolution = new IntXY(1024, 1024);
    this.zoom = intFromFilename(filename, 2);
    this.tileCoordinate = new IntXY(intFromFilename(filename, 3), intFromFilename(filename, 4));
  }

  private int intFromFilename(String filename, int groupPosition) {
    var matcher = filenamePattern.matcher(filename);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "File name does not follow expected pattern, filename=" + filename);
    }

    return parseInt(matcher.group(groupPosition));
  }
}
