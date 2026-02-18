package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.DelimitationObjectType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.mapper.AreaPictureDetailsMapper;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeatureAddressConverter
    implements BiFunction<String, DelimitationObjectType, Feature> {
  private final AreaPictureApi areaPictureApi;
  private final AreaPictureDetailsMapper areaPictureDetailsMapper;
  private final String adminApiKey;

  public FeatureAddressConverter(
      AreaPictureApi areaPictureApi,
      AreaPictureDetailsMapper areaPictureDetailsMapper,
      @Value("${admin.api.key}") String adminApiKey) {
    this.areaPictureApi = areaPictureApi;
    this.areaPictureDetailsMapper = areaPictureDetailsMapper;
    this.adminApiKey = adminApiKey;
  }

  @Override
  public Feature apply(String address, DelimitationObjectType delimitationObjectType) {
    if (BUILDING.equals(delimitationObjectType)) {
      var areaPictureId = randomUUID().toString();
      var crupdateAreaPictureDetails =
          areaPictureDetailsMapper.toCrupdateAreaPictureDetails(address);
      var areaPictureDetails =
          areaPictureApi.crupdateAreaPictureDetails(
              areaPictureId, crupdateAreaPictureDetails, adminApiKey);
      return areaPictureDetailsMapper.toFeature(areaPictureDetails, address);
    }
    throw new NotImplementedException(
        "Unable to convert address to Feature for delimitationObjectType "
            + delimitationObjectType);
  }
}
