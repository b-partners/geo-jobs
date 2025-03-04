package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.ZoomLevel;
import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import org.springframework.stereotype.Component;

@Component
public class ZoomMapper {
  public ZoomLevel toRest(ArcgisImageZoom zoom) {
    return ZoomLevel.valueOf(zoom.toString());
  }

  public ArcgisImageZoom toDomain(ZoomLevel zoomLevel) {
    return ArcgisImageZoom.valueOf(zoomLevel.name());
  }
}
