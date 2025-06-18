package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.service.event.TileExtendedImageRequestedService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PointExtendedImageRequestTest {
  EventProducer eventProducer = mock(EventProducer.class);
  CentroidGeometryRetriever centroidGeometryRetriever = mock(CentroidGeometryRetriever.class);
  TileExtendedImageRequestedService tileExtendedImageRequestedService =
      mock(TileExtendedImageRequestedService.class);
  PointExtendedImageRequest subject;

  FeatureGeometry geometry;
  Feature feature;
  Point point;

  @BeforeEach
  void setup() {
    subject =
        new PointExtendedImageRequest(
            eventProducer, centroidGeometryRetriever, tileExtendedImageRequestedService);
    point = new Point().coordinates(List.of(BigDecimal.valueOf(1.23), BigDecimal.valueOf(4.56)));
    geometry = new FeatureGeometry(point);
    feature = new Feature().geometry(geometry);
  }

  @Test
  void apply_sync_mode_true() {
    when(centroidGeometryRetriever.apply(any())).thenReturn(point);

    var actual = subject.apply(feature, "layer", true);

    assertNotNull(actual);
    assertEquals(Feature.TypeEnum.FEATURE, actual.getType());
    assertNotNull(actual.getGeometry());

    verify(tileExtendedImageRequestedService).accept(any(TileExtendedImageRequested.class));
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void apply_centroidGeometryRetriever_is_null() {
    when(centroidGeometryRetriever.apply(any())).thenReturn(null);

    var actual = subject.apply(feature, "layer", true);

    assertNotNull(actual);
    assertEquals(Feature.TypeEnum.FEATURE, actual.getType());
    assertNull(actual.getGeometry());

    verify(tileExtendedImageRequestedService, never()).accept(any());
    verify(eventProducer, never()).accept(any());
  }
}
