package app.bpartners.geojobs.service.threed.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ThreedApiException extends RuntimeException {
  private final HttpStatusCode statusCode;
  private final String apiError;

  public ThreedApiException(HttpStatusCode statusCode, String apiError, String message) {
    super(message);
    this.statusCode = statusCode;
    this.apiError = apiError;
  }

  public ThreedApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = null;
    this.apiError = null;
  }
}
