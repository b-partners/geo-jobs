package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationTaskRetrievingSubmittedService
    implements Consumer<AnnotationTaskRetrievingSubmitted> {
  private final AnnotationService annotationService;

  @Override
  public void accept(AnnotationTaskRetrievingSubmitted annotationTaskRetrievingSubmitted) {
    var jobId = annotationTaskRetrievingSubmitted.getJobId();
    var firstAnnotationJobId = annotationTaskRetrievingSubmitted.getFirstAnnotationJobId();
    var lastAnnotationJobId = annotationTaskRetrievingSubmitted.getLastAnnotationJobId();
    var firstAnnotationJob = annotationService.getAnnotationJobById(firstAnnotationJobId);
    var lastAnnotationJob = annotationService.getAnnotationJobById(lastAnnotationJobId);
    annotationService.fireTasks(jobId, firstAnnotationJobId, firstAnnotationJob.getImagesWidth());
    annotationService.fireTasks(jobId, lastAnnotationJobId, lastAnnotationJob.getImagesWidth());
  }
}
