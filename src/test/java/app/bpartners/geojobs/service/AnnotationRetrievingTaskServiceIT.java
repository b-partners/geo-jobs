package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotationRetrievingJobRepository;
import app.bpartners.geojobs.repository.AnnotationRetrievingTaskRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AnnotationRetrievingTaskServiceIT extends FacadeIT {
  private static final String JOB_ID = "jobId";
  private static final String TASK_ID = "jobId";
  private static final String ANNOTATION_TASK_ID = "annotationJobId";
  @Autowired AnnotationRetrievingTaskRepository repository;
  @Autowired AnnotationRetrievingJobRepository annotationRetrievingJobRepository;
  @Autowired AnnotationRetrievingTaskService subject;

  AnnotationRetrievingTask task() {
    return AnnotationRetrievingTask.builder()
        .id(TASK_ID)
        .jobId(JOB_ID)
        .annotationTaskId(ANNOTATION_TASK_ID)
        .statusHistory(List.of())
        .submissionInstant(null)
        .build();
  }

  AnnotationRetrievingJob job() {
    return AnnotationRetrievingJob.builder()
        .id(JOB_ID)
        .statusHistory(List.of())
        .submissionInstant(null)
        .build();
  }

  @BeforeEach
  void create_task() {
    annotationRetrievingJobRepository.save(job());
  }

  @Test
  void read_ok() {
    repository.save(task());

    var actual1 = subject.getByRetrievingJobId(JOB_ID);
    var actual2 = subject.getByAnnotationTaskId(ANNOTATION_TASK_ID);

    assertEquals(task(), actual2.toBuilder().submissionInstant(null).build());
    assertTrue(actual1.contains(actual2));
  }

  @Test
  void read_ko() {
    assertThrows(NotFoundException.class, () -> subject.getByAnnotationTaskId("dummy"));
  }
}
