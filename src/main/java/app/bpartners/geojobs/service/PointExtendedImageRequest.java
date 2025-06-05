package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.service.event.TileExtendedImageRequestedService;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointExtendedImageRequest implements TriFunction<Feature, String, Boolean, Feature> {
  private final EventProducer eventProducer;
  private final CentroidGeometryRetriever centroidGeometryRetriever;
  private final TileExtendedImageRequestedService tileExtendedImageRequestedService;

  @Override
  public Feature apply(Feature feature, String layer, Boolean isSynchronous) {
    var geometry = Objects.requireNonNull(feature.getGeometry()).getActualInstance();
    Point point = centroidGeometryRetriever.apply(geometry);

    if (point != null) {
      var pointCoordinates = point.getCoordinates();
      var longitude = pointCoordinates.getFirst();
      var latitude = pointCoordinates.getLast();
      var defaultZoomLevel = HOUSES_0.getZoomLevel();
      var tileExtendedImageRequested =
          new TileExtendedImageRequested(longitude, latitude, defaultZoomLevel, layer);

      if (isSynchronous) {
        tileExtendedImageRequestedService.accept(tileExtendedImageRequested);
      } else {
        eventProducer.accept(List.of(tileExtendedImageRequested));
      }
    }

    return new Feature()
        .type(FEATURE)
        .properties(new HashMap<>())
        .geometry(point == null ? null : new FeatureGeometry(point));
  }
}
