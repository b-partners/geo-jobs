package app.bpartners.geojobs.endpoint.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.RestException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import app.bpartners.geojobs.model.exception.IgnTimeoutException;
import app.bpartners.geojobs.model.exception.ImageSourcesTimeoutException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.model.exception.PaymentRequiredException;
import app.bpartners.geojobs.model.exception.TooManyRequestsException;
import app.bpartners.geojobs.model.exception.UnsupportedDetectionAreaException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class InternalToRestExceptionHandlerTest {
  InternalToRestExceptionHandler subject = new InternalToRestExceptionHandler();

  private static void assertResponse(
      ResponseEntity<RestException> actual, HttpStatus expectedStatus, String expectedMessage) {
    assertEquals(expectedStatus, actual.getStatusCode());
    assertEquals(expectedStatus.toString(), actual.getBody().getType());
    assertEquals(expectedMessage, actual.getBody().getMessage());
  }

  private static HttpMessageNotReadableException notReadable(Throwable cause, String message) {
    return new HttpMessageNotReadableException(message, cause, mock(HttpInputMessage.class));
  }

  @Test
  void bad_request_is_mapped_to_400() {
    var actual = subject.handleBadRequest(new BadRequestException("invalid"));

    assertResponse(actual, HttpStatus.BAD_REQUEST, "invalid");
  }

  @Test
  void missing_request_parameter_is_mapped_to_400() {
    var actual =
        subject.handleBadRequest(new MissingServletRequestParameterException("page", "int"));

    assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
    assertTrue(actual.getBody().getMessage().contains("page"));
  }

  @Test
  void unsupported_http_method_is_mapped_to_400() {
    var actual = subject.handleBadRequest(new HttpRequestMethodNotSupportedException("DELETE"));

    assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
    assertTrue(actual.getBody().getMessage().contains("DELETE"));
  }

  @Test
  void invalid_format_body_lists_the_fields_with_expected_and_received_types() {
    var cause = InvalidFormatException.from(null, "bad value", "abc", Integer.class);
    cause.prependPath(new Object(), "age");

    var actual = subject.handleBadRequest(notReadable(cause, "not readable"));

    assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
    assertEquals(
        "[Field 'age' expects a value of type Integer but got 'abc']",
        actual.getBody().getMessage());
  }

  @Test
  void json_mapping_problem_lists_the_faulty_fields_except_unnamed_ones() {
    var cause = JsonMappingException.from((com.fasterxml.jackson.core.JsonParser) null, "broken");
    cause.prependPath(new Object(), "name");
    cause.prependPath(new Object(), "null");

    var actual = subject.handleBadRequest(notReadable(cause, "not readable"));

    assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
    assertEquals("[Problem with field: 'name']", actual.getBody().getMessage());
  }

  @Test
  void unreadable_body_without_known_cause_reports_the_exception_message() {
    var actual = subject.handleBadRequest(notReadable(null, "Required request body is missing"));

    assertResponse(actual, HttpStatus.BAD_REQUEST, "Required request body is missing");
  }

  @Test
  void argument_type_mismatch_names_the_parameter_expected_type_and_value() {
    var mismatch = mock(MethodArgumentTypeMismatchException.class);
    when(mismatch.getName()).thenReturn("zoom");
    when(mismatch.getValue()).thenReturn("far");
    when(mismatch.getRequiredType()).thenAnswer(invocation -> Integer.class);

    var actual = subject.handleConversionFailed(mismatch);

    assertResponse(
        actual,
        HttpStatus.BAD_REQUEST,
        "Parameter 'zoom' should be of type 'Integer' but got 'far'");
  }

  @Test
  void argument_type_mismatch_without_required_type_reports_unknown_type() {
    var mismatch = mock(MethodArgumentTypeMismatchException.class);
    when(mismatch.getName()).thenReturn("zoom");
    when(mismatch.getValue()).thenReturn(null);
    when(mismatch.getRequiredType()).thenReturn(null);

    var actual = subject.handleConversionFailed(mismatch);

    assertResponse(
        actual,
        HttpStatus.BAD_REQUEST,
        "Parameter 'zoom' should be of type 'unknown' but got 'null'");
  }

  @Test
  void too_many_requests_is_mapped_to_429() {
    var actual = subject.handleTooManyRequests(new TooManyRequestsException("slow down"));

    assertResponse(actual, HttpStatus.TOO_MANY_REQUESTS, "slow down");
  }

  @Test
  void payment_required_is_mapped_to_402() {
    var actual = subject.handlePaymentRequired(new PaymentRequiredException("no credit"));

    assertResponse(actual, HttpStatus.PAYMENT_REQUIRED, "no credit");
  }

  @Test
  void forbidden_is_mapped_to_403() {
    var actual = subject.handleDefault(new ForbiddenException("denied"));

    assertResponse(actual, HttpStatus.FORBIDDEN, "denied");
  }

  @Test
  void not_found_is_mapped_to_404() {
    var actual = subject.handleNotFound(new NotFoundException("missing"));

    assertResponse(actual, HttpStatus.NOT_FOUND, "missing");
  }

  @Test
  void image_sources_timeout_is_mapped_to_504() {
    var actual = subject.handleTimeout(new ImageSourcesTimeoutException("no image"));

    assertResponse(actual, HttpStatus.GATEWAY_TIMEOUT, "no image");
  }

  @Test
  void gateway_timeout_is_mapped_to_504() {
    var actual = subject.handleGatewayTimeout(new GatewayTimeoutException("too slow"));

    assertResponse(actual, HttpStatus.GATEWAY_TIMEOUT, "too slow");
  }

  @Test
  void ign_timeout_is_mapped_to_504() {
    var actual = subject.handleTimeout(new IgnTimeoutException("ign too slow"));

    assertResponse(actual, HttpStatus.GATEWAY_TIMEOUT, "ign too slow");
  }

  @Test
  void not_implemented_is_mapped_to_501() {
    var actual = subject.handleNotFound(new NotImplementedException("later"));

    assertResponse(actual, HttpStatus.NOT_IMPLEMENTED, "later");
  }

  @Test
  void unsupported_detection_area_is_mapped_to_501() {
    var actual =
        subject.handleUnsupportedDetectionArea(
            new UnsupportedDetectionAreaException("area too large", 2_000_000.0));

    assertResponse(actual, HttpStatus.NOT_IMPLEMENTED, "area too large");
  }

  @Test
  void any_other_exception_is_mapped_to_500() {
    var actual = subject.handleDefault(new IllegalStateException("boom"));

    assertResponse(actual, HttpStatus.INTERNAL_SERVER_ERROR, "boom");
  }
}
