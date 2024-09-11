package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_DEGREE;

import app.bpartners.geojobs.endpoint.rest.model.DetectionSurfaceUnit;
import app.bpartners.geojobs.repository.model.SurfaceUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionSurfaceUnitMapper {
  public DetectionSurfaceUnit toRest(SurfaceUnit domain) {
    return switch (domain) {
      case SQUARE_DEGREE -> DetectionSurfaceUnit.SQUARE_DEGREE;
    };
  }

  public SurfaceUnit toDomain(DetectionSurfaceUnit domain) {
    return switch (domain) {
      case DetectionSurfaceUnit.SQUARE_DEGREE -> SQUARE_DEGREE;
    };
  }
}
