package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.model.CreateCityJSONRequest;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDRequest;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateCityJSONRequestValidator implements Consumer<CreateCityJSONRequest> {
  private static final int MAX_ROOFS_COUNT = 5;

  @Override
  public void accept(CreateCityJSONRequest request) {
    if (request.getDelimitations() == null || request.getDelimitations().isEmpty()) {
      throw new BadRequestException(
          "CityJSONRequest.delimitations is mandatory and cannot be empty");
    }

    if (request.getDelimitations().size() > MAX_ROOFS_COUNT) {
      throw new BadRequestException(
          "Requests with more than " + MAX_ROOFS_COUNT + " delimitations are not supported yet.");
    }
  }

  public void accept(ThreeDRequest request) {
    if (request.getDelimitations() == null || request.getDelimitations().isEmpty()) {
      throw new BadRequestException(
          "CityJSONRequest.delimitations is mandatory and cannot be empty");
    }

    if (request.getDelimitations().size() > MAX_ROOFS_COUNT) {
      throw new BadRequestException(
          "Requests with more than " + MAX_ROOFS_COUNT + " delimitations are not supported yet.");
    }
  }
}
