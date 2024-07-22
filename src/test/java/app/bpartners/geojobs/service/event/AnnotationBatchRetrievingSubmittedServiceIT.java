package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.gen.annotator.endpoint.rest.model.Annotation;
import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.AnnotationBatchRetrievingSubmitted;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AnnotationBatchRetrievingSubmittedServiceIT extends FacadeIT {
  private static final String MOCK_JOB_ID = "mock_job_id";
  private static final String MOCK_ANNOTATION_JOB_ID = "mock_annotation_job_id";
  private static final String MOCK_TASK_ID = "mock_task_id";
  @MockBean AnnotationService annotationService;
  @MockBean HumanDetectedTileService humanDetectedTileService;
  @Autowired AnnotationBatchRetrievingSubmittedService subject;

  AnnotationBatchRetrievingSubmitted submitted() {
    return AnnotationBatchRetrievingSubmitted.builder()
        .jobId(MOCK_JOB_ID)
        .annotationJobId(MOCK_ANNOTATION_JOB_ID)
        .taskId(MOCK_TASK_ID)
        .xTile(50000)
        .yTile(12000)
        .zoom(20)
        .imageSize(1024)
        .build();
  }

  AnnotationBatch annotationBatch() {
    return new AnnotationBatch().id(randomUUID().toString()).annotations(List.of(annotation()));
  }

  private Polygon polygon() {
    Point point1 = new Point().x(300.0).y(400.0);
    Point point2 = new Point().x(350.0).y(450.0);
    Point point3 = new Point().x(400.0).y(500.0);
    Point point4 = new Point().x(450.0).y(550.0);
    Point point5 = new Point().x(500.0).y(600.0);
    Point point6 = new Point().x(550.0).y(650.0);
    Point point7 = new Point().x(600.0).y(700.0);
    List<Point> points = List.of(point1, point2, point3, point4, point5, point6, point7, point1);
    Polygon polygon = new Polygon();
    polygon.points(points);
    return polygon;
  }

  Annotation annotation() {
    return new Annotation()
        .id(randomUUID().toString())
        .taskId(MOCK_TASK_ID)
        .label(new Label().id(randomUUID().toString()).name("PATHWAY").color("GREEN"))
        .polygon(polygon());
  }

  @Test
  void accept_ok() {
    when(annotationService.getAnnotations(MOCK_ANNOTATION_JOB_ID, MOCK_TASK_ID))
        .thenReturn(List.of(annotationBatch()));

    subject.accept(submitted());

    verify(humanDetectedTileService, times(1)).saveAll(anyList());
  }
}
