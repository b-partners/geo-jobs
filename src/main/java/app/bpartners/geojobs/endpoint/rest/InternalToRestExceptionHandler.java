package app.bpartners.geojobs.endpoint.rest;

import app.bpartners.geojobs.endpoint.rest.model.RestException;
import app.bpartners.geojobs.model.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class InternalToRestExceptionHandler {

  @ExceptionHandler(value = {BadRequestException.class})
  ResponseEntity<RestException> handleBadRequest(BadRequestException e) {
    log.info("Bad request", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(value = {MissingServletRequestParameterException.class})
  ResponseEntity<RestException> handleBadRequest(MissingServletRequestParameterException e) {
    log.info("Missing parameter", e);
    return handleBadRequest(new BadRequestException(e.getMessage()));
  }

  @ExceptionHandler(value = {HttpRequestMethodNotSupportedException.class})
  ResponseEntity<RestException> handleBadRequest(HttpRequestMethodNotSupportedException e) {
    log.info("Unsupported method for this endpoint", e);
    return handleBadRequest(new BadRequestException(e.getMessage()));
  }

  @ExceptionHandler(value = {HttpMessageNotReadableException.class})
  ResponseEntity<RestException> handleBadRequest(HttpMessageNotReadableException e) {
    log.info("Missing required body", e);
    return handleBadRequest(new BadRequestException(e.getMessage()));
  }

  @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
  ResponseEntity<RestException> handleConversionFailed(MethodArgumentTypeMismatchException e) {
    log.info("Conversion failed", e);
    String message = e.getCause().getCause().getMessage();
    return handleBadRequest(new BadRequestException(message));
  }

  @ExceptionHandler(value = {TooManyRequestsException.class})
  ResponseEntity<RestException> handleTooManyRequests(TooManyRequestsException e) {
    log.info("Too many requests", e);
    return new ResponseEntity<>(
        toRest(e, HttpStatus.TOO_MANY_REQUESTS), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(value = {ForbiddenException.class})
  ResponseEntity<RestException> handleDefault(ForbiddenException e) {
    log.error("Authentication error", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(value = {NotFoundException.class})
  ResponseEntity<RestException> handleNotFound(NotFoundException e) {
    log.info("Not found", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(value = {NotImplementedException.class})
  ResponseEntity<RestException> handleNotFound(NotImplementedException e) {
    log.info("Not implemented", e);
    return new ResponseEntity<>(toRest(e, HttpStatus.NOT_IMPLEMENTED), HttpStatus.NOT_IMPLEMENTED);
  }

  @ExceptionHandler(value = {java.lang.Exception.class})
  ResponseEntity<RestException> handleDefault(java.lang.Exception e) {
    log.error("Internal error", e);
    return new ResponseEntity<>(
        toRest(e, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private RestException toRest(java.lang.Exception e, HttpStatus status) {
    var restException = new RestException();
    restException.setType(status.toString());
    restException.setMessage(e.getMessage());
    return restException;
  }
}
