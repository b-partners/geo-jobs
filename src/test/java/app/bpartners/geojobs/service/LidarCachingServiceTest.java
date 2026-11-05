package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.CreateLidarCaching;
import app.bpartners.geojobs.endpoint.rest.model.LidarCaching;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import app.bpartners.geojobs.validator.CreateLidarCachingValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LidarCachingServiceTest {
  private final ThreedApiClient threedApiClientMock = mock();
  private final LidarCachingService subject = new LidarCachingService(threedApiClientMock);
  private final CreateLidarCachingValidator validator = new CreateLidarCachingValidator();

  @Test
  void should_return_threed_lidar_caching() {
    var expected = new LidarCaching().id("address-1");
    when(threedApiClientMock.cacheLidar(eq("address-1"), any())).thenReturn(expected);

    assertSame(expected, subject.cache("address-1", new CreateLidarCaching()));
  }

  @Test
  void should_report_a_refused_point_as_bad_request() {
    when(threedApiClientMock.cacheLidar(any(), any()))
        .thenThrow(
            new ThreedApiException(
                HttpStatus.BAD_REQUEST, "CLIENT_EXCEPTION", "bad latitude", "Threed API error"));

    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.cache("address-1", new CreateLidarCaching()));

    assertEquals("bad latitude", actual.getMessage());
  }

  @Test
  void should_let_a_threed_failure_through() {
    var failure =
        new ThreedApiException(
            HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_EXCEPTION", "boom", "Threed API error");
    when(threedApiClientMock.cacheLidar(any(), any())).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            ThreedApiException.class, () -> subject.cache("address-1", new CreateLidarCaching())));
  }

  @Test
  void validator_refuses_a_missing_or_out_of_range_point() {
    assertThrows(BadRequestException.class, () -> validator.accept(new CreateLidarCaching()));
    assertThrows(
        BadRequestException.class,
        () -> validator.accept(new CreateLidarCaching().latitude(91.0).longitude(2.0)));
    validator.accept(new CreateLidarCaching().latitude(48.8566).longitude(2.3522));
  }
}
