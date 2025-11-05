package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import app.bpartners.geojobs.endpoint.rest.model.CityJSONRequestStatus;

public class CityJSONRequestStatusMapper {
  public static CityJSONRequestStatus toRest(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus status) {
    return switch (status) {
      case FINISHED -> CityJSONRequestStatus.FINISHED;
      case FAILED -> CityJSONRequestStatus.FAILED;
      case PROCESSING -> CityJSONRequestStatus.PROCESSING;
    };
  }
}
