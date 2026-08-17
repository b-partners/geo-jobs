package app.bpartners.geojobs.postprocessing;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PLACE_STANDARD;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;

import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Post-processes the objects detected by the STATIONNEMENT model only: the model returns noisy
 * PLACE_STANDARD objects that are far too small to be an actual parking place, and those objects
 * must not be delivered. Objects detected by any other model are left untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationnementPostProcessor implements BiFunction<GeoJson, Detection, GeoJson> {
  public static final double MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER = 9.0;

  private final GeometryConverter geometryConverter;
  private final GeometrySquareMeterArea geometrySquareMeterArea;

  @Override
  public GeoJson apply(GeoJson geoJson, @Nullable Detection detection) {
    if (geoJson == null
        || geoJson.getGeoFeatures() == null
        || detection == null
        || !detection.hasStationnementModelName()) {
      return geoJson;
    }
    var geoFeatures = geoJson.getGeoFeatures();
    var withoutNoise = geoFeatures.stream().filter(geoFeature -> !isNoise(geoFeature)).toList();
    if (withoutNoise.size() == geoFeatures.size()) {
      return geoJson;
    }
    log.info(
        "{} noisy {} object(s) filtered out for detection(id={})",
        geoFeatures.size() - withoutNoise.size(),
        PLACE_STANDARD,
        detection.getId());
    return fromFeatures(withoutNoise);
  }

  private boolean isNoise(GeoJson.GeoFeature geoFeature) {
    if (!isPlaceStandard(geoFeature)) {
      return false;
    }
    var areaInSquareMeter = areaInSquareMeter(geoFeature);
    return areaInSquareMeter != null && areaInSquareMeter < MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER;
  }

  private boolean isPlaceStandard(GeoJson.GeoFeature geoFeature) {
    var properties = geoFeature.getProperties();
    if (properties == null) {
      return false;
    }
    var label = properties.get("label");
    return label != null && PLACE_STANDARD.name().equalsIgnoreCase(label.toString());
  }

  private Double areaInSquareMeter(GeoJson.GeoFeature geoFeature) {
    var geometry = geoFeature.getGeometry();
    if (geometry == null || geometry.getCoordinates() == null) {
      return null;
    }
    try {
      return geometrySquareMeterArea.apply(geometryConverter.apply(geometry.getCoordinates()));
    } catch (RuntimeException e) {
      log.warn(
          "Unable to compute area of detected {} object, it is not filtered out: {}",
          PLACE_STANDARD,
          e.getMessage());
      return null;
    }
  }
}
