package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CreateAnnotatedTaskSubmittedService implements Consumer<CreateAnnotatedTaskSubmitted> {
  private final AnnotationService annotationService;

  @Override
  public void accept(CreateAnnotatedTaskSubmitted createAnnotatedTaskSubmitted) {
    var jobId = createAnnotatedTaskSubmitted.getAnnotationJobId();
    var annotatedTask = createAnnotatedTaskSubmitted.getCreateAnnotatedTask();
    try {
      annotationService.addAnnotationTask(jobId, annotatedTask);
      log.error(
          "[DEBUG] AnnotatedTask(id={}) sent to annotator with jobId = {}",
          annotatedTask.getId(),
          jobId);
    } catch (Exception e) {
      log.error(
          "[DEBUG] Error when adding annotation task CreateAnnotatedTask(id={}) with Exception ="
              + " {}",
          annotatedTask.getId(),
          e.getMessage());
      // TODO: add retryer CreateAnnotatedTaskExtractedFailed
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
}
