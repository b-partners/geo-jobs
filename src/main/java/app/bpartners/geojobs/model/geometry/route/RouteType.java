package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOnly;
import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOrWidth;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
public enum RouteType {
  road(lengthOnly),
  pathway(lengthOrWidth);
  private final ContinuationOrientation continuationOrientation;
}
