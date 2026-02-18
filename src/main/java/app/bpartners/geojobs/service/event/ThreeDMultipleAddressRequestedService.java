package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;

import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson.CityJSONRequestMapper;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDRequest;
import app.bpartners.geojobs.service.CityJSONRequestService;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThreeDMultipleAddressRequestedService
    implements Consumer<ThreeDMultipleAddressRequested> {
  private final FeatureAddressConverter featureAddressConverter;
  private final CityJSONRequestMapper cityJSONRequestMapper;
  private final CityJSONRequestService cityJSONRequestService;

  @Override
  public void accept(ThreeDMultipleAddressRequested requestEvent) {
    var requestIdentifier = requestEvent.getRequestIdentifier();
    var addresses = requestEvent.getAddresses();
    var communityOwnerId = requestEvent.getCommunityOwnerId();

    var convertedAddressesToDelimitations =
        addresses.stream()
            .map(addressValue -> featureAddressConverter.apply(addressValue, BUILDING))
            .map(FeatureMapper::toRestFeature)
            .toList();

    var threeDRequest = new ThreeDRequest().delimitations(convertedAddressesToDelimitations);
    var toProcess =
        cityJSONRequestMapper.createToDomain(requestIdentifier, threeDRequest, communityOwnerId);

    cityJSONRequestService.process(toProcess);
  }
}
