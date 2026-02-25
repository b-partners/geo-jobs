package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.NEIGHBOUR_SIZE;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DetectionBoundaryMerger {

  public Set<LatLonPolygon> apply(Set<TiledPolygon> tiledPolygons) {
    if (tiledPolygons.isEmpty()) {
      return Set.of();
    }
    return tiledPolygons.stream()
        .collect(Collectors.groupingBy(this::getDetectableType))
        .entrySet()
        .stream()
        .flatMap(
            entry -> {
              var detectableType = entry.getKey();
              var merger =
                  new BoundaryMerger(detectableType.getMinAreaThreshold(), NEIGHBOUR_SIZE, true);
              return merger.apply(Set.copyOf(entry.getValue()), detectableType).stream();
            })
        .collect(Collectors.toSet());
  }

  public Set<TiledPolygon> applyVgg(VGG vgg) {
    var toUnify = toTiledPolygons(TilingConf.getDefaultInstance(), vgg);
    if (toUnify.isEmpty()) return Set.of();
    var origin = toUnify.iterator().next().originTile();
    return apply(toUnify).stream()
        .map(latLon -> latLon.tiledPolygon(TilingConf.getDefaultInstance(), origin))
        .collect(toSet());
  }

  public Set<TiledPolygon> applyVggSet(Collection<VGG> vggSet) {
    var toUnify =
        vggSet.stream()
            .flatMap(vgg -> toTiledPolygons(TilingConf.getDefaultInstance(), vgg).stream())
            .collect(toSet());
    if (toUnify.isEmpty()) return Set.of();
    var origin = toUnify.iterator().next().originTile();
    return apply(toUnify).stream()
        .map(latLon -> latLon.tiledPolygon(TilingConf.getDefaultInstance(), origin))
        .collect(toSet());
  }

  private Set<TiledPolygon> toTiledPolygons(TilingConf tilingConf, VGG vgg) {
    return TiledPolygon.toTiledPolygons(tilingConf, vgg, false);
  }

  private DetectableType getDetectableType(TiledPolygon tp) {
    var userData = (Map<String, Object>) tp.polygon().getUserData();
    if (userData != null && userData.containsKey("label")) {
      var label = userData.get("label").toString().toUpperCase();
      try {
        return DetectableType.valueOf(label);
      } catch (IllegalArgumentException e) {
        log.warn("Unknown detectable type from label: {}", label);
      }
    }
    try {
      return DetectableType.valueOf(tp.type().name().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Unknown detectable type from ObjectType: {}", tp.type());
      return DetectableType.BACKGROUND;
    }
  }
}
