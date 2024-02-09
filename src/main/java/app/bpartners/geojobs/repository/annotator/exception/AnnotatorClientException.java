package app.bpartners.geojobs.repository.annotator.exception;

public class AnnotatorClientException extends RuntimeException {

  public static final String DEFAULT_EXCEPTION_MESSAGE =
      "[BPartners Annotator API] Exception occurred when performing request : ";

  public AnnotatorClientException(String message) {
    super(DEFAULT_EXCEPTION_MESSAGE + message);
  }

  public AnnotatorClientException(Exception e) {
    super(DEFAULT_EXCEPTION_MESSAGE + e);
  }
}
