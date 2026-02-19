package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson.CityJSONRequestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CityJSONRequestOwnerAuthorizer;
import app.bpartners.geojobs.endpoint.rest.validator.CreateCityJSONRequestValidator;
import app.bpartners.geojobs.endpoint.rest.validator.ThreeDAddressesRequestValidator;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSONRequestService;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CityJSONController {
  private final CityJSONRequestMapper cityJSONRequestMapper;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final AuthProvider authProvider;
  private final CityJSONRequestService cityJSONRequestService;
  private final CityJSONRequestOwnerAuthorizer cityJSONRequestOwnerAuthorizer;
  private final CreateCityJSONRequestValidator createCityJSONRequestValidator;
  private final ThreeDAddressesRequestValidator threeDAddressesRequestValidator;
  private final FeatureAddressConverter featureAddressConverter;
  private final EventProducer eventProducer;

  @GetMapping("/3d/{id}")
  public ThreeDResponseStatus getRequested3DFileById(@PathVariable(name = "id") String requestId) {
    var communityOwnerId = getCommunityAuthorizationId();

    cityJSONRequestOwnerAuthorizer.accept(requestId, communityOwnerId, authProvider.getPrincipal());

    return cityJSONRequestMapper.toRestThreeDResponseStatus(
        cityJSONRequestService.getById(requestId));
  }

  @PostMapping("/3d/{id}")
  public ThreeDResponseStatus request3DFileOnDelimitations(
      @RequestBody ThreeDRequest threeDRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    createCityJSONRequestValidator.accept(threeDRequest);

    var communityOwnerId = getCommunityAuthorizationId();
    var toProcess =
        cityJSONRequestMapper.createToDomain(requestIdentifier, threeDRequest, communityOwnerId);
    cityJSONRequestOwnerAuthorizer.accept(
        toProcess.getId(), communityOwnerId, authProvider.getPrincipal());

    return cityJSONRequestMapper.toRestThreeDResponseStatus(
        cityJSONRequestService.process(toProcess));
  }

  @PostMapping("/3d/{id}/addresses")
  public ThreeDResponseStatus request3DFileOnAddresses(
      @RequestBody ThreeDAddressesRequest threeDRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    threeDAddressesRequestValidator.accept(threeDRequest);
    var communityOwnerId = getCommunityAuthorizationId();
    if (threeDRequest.getAddresses().size() == 1) {
      var convertedAddressesToDelimitations =
          threeDRequest.getAddresses().stream()
              .map(AddressFullText::getFullText)
              .map(addressValue -> featureAddressConverter.apply(addressValue, BUILDING))
              .map(FeatureMapper::toRestFeature)
              .toList();

      var request = new ThreeDRequest().delimitations(convertedAddressesToDelimitations);
      var toProcess =
          cityJSONRequestMapper.createToDomain(requestIdentifier, request, communityOwnerId);

      return cityJSONRequestMapper.toRestThreeDResponseStatus(
          cityJSONRequestService.process(toProcess));
    }
    var savedRequest =
        cityJSONRequestService.processAddressRequest(
            requestIdentifier,
            threeDRequest.getAddresses().stream().map(AddressFullText::getFullText).toList(),
            communityOwnerId);

    return cityJSONRequestMapper.toRestThreeDResponseStatus(savedRequest);
  }

  @PutMapping("/city-jsons/{id}/process")
  public CityJSONRequest processCityJSONRequest(
      @RequestBody CreateCityJSONRequest createCityJSONRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    createCityJSONRequestValidator.accept(createCityJSONRequest);

    var communityOwnerId = getCommunityAuthorizationId();
    var toProcess =
        cityJSONRequestMapper.createToDomain(
            requestIdentifier, createCityJSONRequest, communityOwnerId);
    cityJSONRequestOwnerAuthorizer.accept(
        toProcess.getId(), communityOwnerId, authProvider.getPrincipal());

    return cityJSONRequestMapper.toRest(cityJSONRequestService.process(toProcess));
  }

  @GetMapping("/city-jsons/{id}")
  public CityJSONRequest getById(@PathVariable(name = "id") String requestId) {
    var communityOwnerId = getCommunityAuthorizationId();

    cityJSONRequestOwnerAuthorizer.accept(requestId, communityOwnerId, authProvider.getPrincipal());

    return cityJSONRequestMapper.toRest(cityJSONRequestService.getById(requestId));
  }

  private String getCommunityAuthorizationId() {
    var communityAuthorization =
        communityAuthorizationRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    return communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
  }
}
