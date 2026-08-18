package app.bpartners.geojobs.postprocessing;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PARKING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PLACE_STANDARD;
import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Post-processes the objects detected by the STATIONNEMENT model only: the model returns noisy
 * PLACE_STANDARD objects that are far too small to be an actual parking place, and those objects
 * must not be delivered. The area of the delivered PLACE_STANDARD and PARKING objects is added to
 * their properties, so that the consumer is able to filter them out on its side too. Objects
 * detected by any other model are left untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationnementPostProcessor implements BiFunction<GeoJson, Detection, GeoJson> {
  public static final double MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER = 9.0;
  public static final String AREA_IN_SQUARE_METER_PROPERTY = "area_in_m_2";
  public static final Set<DetectableType> AREA_DELIVERED_TYPES = Set.of(PLACE_STANDARD, PARKING);

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
    var withoutNoise = new ArrayList<GeoJson.GeoFeature>();
    var withDeliveredArea = 0;
    for (var geoFeature : geoFeatures) {
      if (!hasAreaDelivered(geoFeature)) {
        withoutNoise.add(geoFeature);
        continue;
      }
      withDeliveredArea++;
      var areaInSquareMeter = areaInSquareMeter(geoFeature);
      geoFeature.getProperties().put(AREA_IN_SQUARE_METER_PROPERTY, areaInSquareMeter);
      if (!isPlaceStandard(geoFeature) || !isNoise(areaInSquareMeter)) {
        withoutNoise.add(geoFeature);
      }
    }
    if (withDeliveredArea == 0) {
      return geoJson;
    }
    if (withoutNoise.size() < geoFeatures.size()) {
      log.info(
          "{} noisy {} object(s) filtered out for detection(id={})",
          geoFeatures.size() - withoutNoise.size(),
          PLACE_STANDARD,
          detection.getId());
    }
    return fromFeatures(withoutNoise);
  }

  private boolean isNoise(@Nullable Double areaInSquareMeter) {
    return areaInSquareMeter != null && areaInSquareMeter < MIN_PLACE_STANDARD_AREA_IN_SQUARE_METER;
  }

  private boolean hasAreaDelivered(GeoJson.GeoFeature geoFeature) {
    return AREA_DELIVERED_TYPES.stream().anyMatch(type -> hasLabel(geoFeature, type));
  }

  private boolean isPlaceStandard(GeoJson.GeoFeature geoFeature) {
    return hasLabel(geoFeature, PLACE_STANDARD);
  }

  private boolean hasLabel(GeoJson.GeoFeature geoFeature, DetectableType detectableType) {
    var properties = geoFeature.getProperties();
    if (properties == null) {
      return false;
    }
    var label = properties.get("label");
    return label != null && detectableType.name().equalsIgnoreCase(label.toString());
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
          "Unable to compute area of detected {} object, it is delivered without area and not"
              + " filtered out: {}",
          geoFeature.getProperties().get("label"),
          e.getMessage());
      return null;
    }
  }
}
