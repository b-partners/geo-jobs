package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.service.annotator.AnnotatedTaskService;
import app.bpartners.geojobs.service.annotator.AnnotatedTaskStatusService;
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
  private final AnnotatedTaskStatusService annotatedTaskStatusService;
  private final AnnotatedTaskService annotatedTaskService;

  @Override
  public void accept(CreateAnnotatedTaskSubmitted createAnnotatedTaskSubmitted) {
    var jobId = createAnnotatedTaskSubmitted.getAnnotationJobId();
    var annotatedTaskSent = createAnnotatedTaskSubmitted.getCreateAnnotatedTask();
    var persistedAnnotatedTask =
        annotatedTaskService.getByCreateAnnotatedTaskId(annotatedTaskSent.getId());
    annotatedTaskStatusService.process(persistedAnnotatedTask);
    try {
      annotationService.addAnnotationTask(jobId, annotatedTaskSent);
      log.error(
          "[DEBUG] AnnotatedTask(id={}) sent to annotator with jobId = {}",
          annotatedTaskSent.getId(),
          jobId);
    } catch (Exception e) {
      log.error(
          "[DEBUG] Error when adding annotation task CreateAnnotatedTask(id={}) with Exception ="
              + " {}",
          annotatedTaskSent.getId(),
          e.getMessage());
      annotatedTaskStatusService.fail(persistedAnnotatedTask);
      // TODO: add retryer CreateAnnotatedTaskExtractedFailed
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
    annotatedTaskStatusService.succeed(persistedAnnotatedTask);
  }
}
