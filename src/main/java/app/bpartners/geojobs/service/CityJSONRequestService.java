package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.PROCESSING;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityJSONRequestService {
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final EventProducer eventProducer;

  public CityJSONRequest processAddressRequest(
      String requestIdentifier, List<String> addresses, String communityOwnerId) {
    var optionalRequest =
        cityJSONRequestRepository.findByIdAndCommunityOwnerId(requestIdentifier, communityOwnerId);
    if (optionalRequest.isPresent()) {
      throw new BadRequestException(
          "Process request with id "
              + requestIdentifier
              + " can not be either updated or processed again");
    }
    var savedRequest =
        cityJSONRequestRepository.save(
            CityJSONRequest.builder()
                .id(requestIdentifier)
                .status(PROCESSING)
                .communityOwnerId(communityOwnerId)
                .build());
    eventProducer.accept(
        List.of(new ThreeDMultipleAddressRequested(savedRequest.getId(), addresses)));
    return savedRequest;
  }

  public CityJSONRequest process(CityJSONRequest cityJSONRequest) {
    var requestIdentifier = cityJSONRequest.getId();
    var optionalRequest = cityJSONRequestRepository.findByIdAndCommunityOwnerId(
            requestIdentifier,
            cityJSONRequest.getCommunityOwnerId());

    if (optionalRequest.isPresent()) {
      throw new BadRequestException(
              "Process request with id "
                      + requestIdentifier
                      + " can not be either updated or processed again");
    }

    var saved =
        cityJSONRequestRepository.save(cityJSONRequest.toBuilder().status(PROCESSING).build());

    eventProducer.accept(
        List.of(CityJSONRequestCreated.builder().requestId(saved.getId()).build()));

    return saved;
  }

  public CityJSONRequest getByIdAndCommunityOwnerId(String requestId, String communityOwnerId) {
    var optionalRequest =
        cityJSONRequestRepository.findByIdAndCommunityOwnerId(requestId, communityOwnerId);
    if (optionalRequest.isEmpty()) {
      throw new NotFoundException(String.format("CityJSONRequest.id=%s was not found", requestId));
    }

    return optionalRequest.get();
  }
}
