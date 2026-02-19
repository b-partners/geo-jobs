package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreeDMultipleAddressRequestedService
    implements Consumer<ThreeDMultipleAddressRequested> {
  private final FeatureAddressConverter featureAddressConverter;
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(ThreeDMultipleAddressRequested requestEvent) {
    var requestIdentifier = requestEvent.getRequestIdentifier();
    var persistedRequest = cityJSONRequestRepository.findById(requestIdentifier).orElseThrow();
    var addresses = requestEvent.getAddresses();
    List<Feature> convertedFeatures;
    try {
      convertedFeatures =
          addresses.stream()
              .map(addressValue -> featureAddressConverter.apply(addressValue, BUILDING))
              .toList();
    } catch (ApiException e) {
      log.error(
          "Conversion of addresses to features failed with API exception from dashboard {}",
          e.getMessage());
      cityJSONRequestRepository.save(persistedRequest.toBuilder().status(FAILED).build());
      return;
    }

    var saved =
        cityJSONRequestRepository.save(
            persistedRequest.toBuilder().delimitations(convertedFeatures).build());

    eventProducer.accept(
        List.of(CityJSONRequestCreated.builder().requestId(saved.getId()).build()));
  }
}
