package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDRequest;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import org.junit.jupiter.api.Test;

class CityJSONRequestMapperTest {
  CityJSONRequestMapper subject = new CityJSONRequestMapper(mock(BucketComponent.class));

  @Test
  void map_the_analysis_project_of_the_request() {
    var request = threeDRequest().projectKey("session-1").externalReference("crm-42");

    var actual = subject.createToDomain("request-id", request, "community-id");

    assertEquals("session-1", actual.getProjectKey());
    assertEquals("crm-42", actual.getExternalReference());
  }

  @Test
  void map_blank_analysis_project_as_none() {
    var request = threeDRequest().projectKey(" ").externalReference("");

    var actual = subject.createToDomain("request-id", request, "community-id");

    assertNull(actual.getProjectKey());
    assertNull(actual.getExternalReference());
  }

  private static ThreeDRequest threeDRequest() {
    return new ThreeDRequest()
        .delimitationObjectType(DelimitationObjectType.BUILDING_ROOF)
        .delimitationType(USER_DEFINED_DELIMITATION);
  }
}
