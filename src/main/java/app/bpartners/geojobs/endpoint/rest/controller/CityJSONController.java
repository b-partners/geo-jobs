package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson.CityJSONRequestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.model.RasterInfo;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CityJSONRequestValidator;
import app.bpartners.geojobs.endpoint.rest.validator.CreateCityJSONRequestValidator;
import app.bpartners.geojobs.endpoint.rest.validator.ThreeDAddressesRequestValidator;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSONRequestService;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class CityJSONController {
  private final CityJSONRequestMapper cityJSONRequestMapper;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final AuthProvider authProvider;
  private final CityJSONRequestService cityJSONRequestService;
  private final CityJSONRequestValidator cityJSONRequestValidator;
  private final CreateCityJSONRequestValidator createCityJSONRequestValidator;
  private final ThreeDAddressesRequestValidator threeDAddressesRequestValidator;
  private final FeatureAddressConverter featureAddressConverter;
  private final CityJsonTextureService cityJsonTextureService;
  private final FileWriter fileWriter;

  @PostMapping("/3d/texture")
  public byte[] textureCityJSON(
      @RequestPart(value = "cityJson") MultipartFile cityJson,
      @RequestPart(value = "image") MultipartFile image,
      @RequestPart(value = "rasterInfo") RasterInfo rasterInfo)
      throws IOException {
    File cityJsonFile = null;
    File imageFile = null;
    File texturedCityJsonFile = null;
    try {
      cityJsonFile = Files.createTempFile("cityjson-", ".json").toFile();
      imageFile = Files.createTempFile("image-", ".tif").toFile();

      try (var is = cityJson.getInputStream()) {
        Files.copy(is, cityJsonFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      try (var is = image.getInputStream()) {
        Files.copy(is, imageFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }

      texturedCityJsonFile =
          cityJsonTextureService.textureCityJson(
              cityJsonFile,
              imageFile,
              rasterInfo.getTopLeftLon().doubleValue(),
              rasterInfo.getTopLeftLat().doubleValue(),
              rasterInfo.getOnePixelInCentimeters().doubleValue() / 100.0);

      return Files.readAllBytes(texturedCityJsonFile.toPath());
    } finally {
      if (cityJsonFile != null) cityJsonFile.delete();
      if (imageFile != null) imageFile.delete();
      if (texturedCityJsonFile != null) texturedCityJsonFile.delete();
    }
  }

  @GetMapping("/3d/{id}")
  public ThreeDResponseStatus getRequested3DFileById(@PathVariable(name = "id") String requestId) {
    var communityOwnerId = getCommunityAuthorizationId();

    return cityJSONRequestMapper.toRestThreeDResponseStatus(
        cityJSONRequestService.getByIdAndCommunityOwnerId(requestId, communityOwnerId));
  }

  @PostMapping("/3d/{id}")
  public ThreeDResponseStatus request3DFileOnDelimitations(
      @RequestBody ThreeDRequest threeDRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    var communityOwnerId = getCommunityAuthorizationId();
    createCityJSONRequestValidator.accept(threeDRequest);
    cityJSONRequestValidator.accept(requestIdentifier, communityOwnerId);

    var toProcess =
        cityJSONRequestMapper.createToDomain(requestIdentifier, threeDRequest, communityOwnerId);

    return cityJSONRequestMapper.toRestThreeDResponseStatus(
        cityJSONRequestService.process(toProcess));
  }

  @PostMapping("/3d/{id}/addresses")
  public ThreeDResponseStatus request3DFileOnAddresses(
      @RequestBody ThreeDAddressesRequest threeDRequest,
      @PathVariable(name = "id") String requestIdentifier) {
    threeDAddressesRequestValidator.accept(threeDRequest);
    var communityOwnerId = getCommunityAuthorizationId();
    cityJSONRequestValidator.accept(requestIdentifier, communityOwnerId);
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

    return cityJSONRequestMapper.toRest(cityJSONRequestService.oldProcess(toProcess));
  }

  @GetMapping("/city-jsons/{id}")
  public CityJSONRequest getById(@PathVariable(name = "id") String requestId) {
    var communityOwnerId = getCommunityAuthorizationId();

    return cityJSONRequestMapper.toRest(
        cityJSONRequestService.getByIdAndCommunityOwnerId(requestId, communityOwnerId));
  }

  private String getCommunityAuthorizationId() {
    var communityAuthorization =
        communityAuthorizationRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    return communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
  }
}
