package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.repository.model.detection.GeoJsonDelimitationType;
import org.springframework.stereotype.Component;

@Component
public class GeoJsonDelimitationTypeMapper {
  public GeoJsonDelimitationType toDomain(CreateDetection.GeoJsonDelimitationTypeEnum rest) {
    return switch (rest) {
      case null -> GeoJsonDelimitationType.ZONE;
      case CreateDetection.GeoJsonDelimitationTypeEnum.ROOF -> GeoJsonDelimitationType.ROOF;
      case CreateDetection.GeoJsonDelimitationTypeEnum.ZONE -> GeoJsonDelimitationType.ZONE;
    };
  }

  public Detection.GeoJsonDelimitationTypeEnum toRest(GeoJsonDelimitationType domain) {
    return switch (domain) {
      case null -> Detection.GeoJsonDelimitationTypeEnum.ZONE;
      case GeoJsonDelimitationType.ROOF -> Detection.GeoJsonDelimitationTypeEnum.ROOF;
      case GeoJsonDelimitationType.ZONE -> Detection.GeoJsonDelimitationTypeEnum.ZONE;
    };
  }
}
