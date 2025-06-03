package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointExtendedImageRequest implements BiFunction<Feature, String, Feature> {
  private final EventProducer eventProducer;
  private final CentroidGeometryRetriever centroidGeometryRetriever;

  @Override
  public Feature apply(Feature feature, String layer) {
    var geometry = Objects.requireNonNull(feature.getGeometry()).getActualInstance();
    Point point = centroidGeometryRetriever.apply(geometry);

    if (point != null) {
      var pointCoordinates = point.getCoordinates();
      var longitude = pointCoordinates.getFirst();
      var latitude = pointCoordinates.getLast();
      var defaultZoomLevel = HOUSES_0.getZoomLevel();

      eventProducer.accept(
          List.of(new TileExtendedImageRequested(longitude, latitude, defaultZoomLevel, layer)));
    }

    return new Feature()
        .type(FEATURE)
        .properties(new HashMap<>())
        .geometry(new FeatureGeometry(point));
  }
}
