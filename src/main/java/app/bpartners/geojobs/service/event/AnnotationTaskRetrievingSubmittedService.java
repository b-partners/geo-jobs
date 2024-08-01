package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationTaskRetrievingSubmittedService
    implements Consumer<AnnotationTaskRetrievingSubmitted> {
  private final AnnotationService annotationService;

  @Override
  public void accept(AnnotationTaskRetrievingSubmitted submitted) {
    var humanZdjId = submitted.getHumanZdjId();
    var annotationJobId = submitted.getAnnotationJobId();
    var imageWidth = submitted.getImageWidth();
    annotationService.fireTasks(humanZdjId, annotationJobId, imageWidth);
  }
}
