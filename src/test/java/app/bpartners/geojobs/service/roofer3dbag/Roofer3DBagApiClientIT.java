package app.bpartners.geojobs.service.roofer3dbag;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("TODO: local use only")
class Roofer3DBagApiClientIT extends FacadeIT {
  @Autowired Roofer3DBagApiClient subject;

  @Test
  void convert_lidar_to_city_json() {
    var geoJsonBuildingBucketUri = "";
    var lidarBucketUris = List.of("");

    var actual =
        subject.generateCityJson(
            CityJsonGenerationRequest.builder()
                .geoJsonBuildingBucketUri(geoJsonBuildingBucketUri)
                .lidarBucketUris(lidarBucketUris)
                .build());

    assertNotNull(actual);
    assertNotNull(actual.getCityJsonUrl());
    assertNotNull(actual.getExpirationDateTime());
    assertTrue(actual.getCityJsonUrl().contains(""));
  }
}
