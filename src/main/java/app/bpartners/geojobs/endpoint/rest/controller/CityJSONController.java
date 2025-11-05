package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson.CityJSONRequestMapper;
import app.bpartners.geojobs.endpoint.rest.model.CityJSONRequest;
import app.bpartners.geojobs.endpoint.rest.model.CreateCityJSONRequest;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSONRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CityJSONController {
  private final CityJSONRequestMapper cityJSONRequestMapper;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final AuthProvider authProvider;
  private final CityJSONRequestService cityJSONRequestService;

  @PutMapping("/city-jsons")
  public CityJSONRequest processCityJSONRequest(
      @RequestBody CreateCityJSONRequest createCityJSONRequest) {
    var communityAuthorization =
        communityAuthorizationRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
    var toProcess = cityJSONRequestMapper.createToDomain(createCityJSONRequest, communityOwnerId);

    return cityJSONRequestMapper.toRest(cityJSONRequestService.process(toProcess));
  }
}
