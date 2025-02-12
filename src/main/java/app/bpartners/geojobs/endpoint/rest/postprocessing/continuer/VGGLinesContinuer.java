package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.newTiledPolygons;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.model.geometry.VGG;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VGGLinesContinuer implements Function<VGG, Set<TiledPolygon>> {
  private final boolean isZXYDotFiletype;
  private final int imgSize;

  @Override
  public Set<TiledPolygon> apply(VGG vgg) {
    var annotations = vgg.values();
    return annotations.stream()
        .map(
            annotation -> {
              var filename = annotation.getFilename();
              var regions = annotation.getRegions();
              return newTiledPolygons(filename, regions, imgSize, isZXYDotFiletype);
            })
        .flatMap(Collection::stream)
        .collect(toSet());
  }
}
