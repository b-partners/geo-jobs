package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotationRetrievingJobRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AnnotationRetrievingJobServiceIT extends FacadeIT {
  private static final String JOB_ID = "jobId";
  private static final String DETECTION_JOB_ID = "detectionJobId";
  private static final String ANNOTATION_JOB_ID = "annotationJobId";
  @Autowired AnnotationRetrievingJobRepository repository;
  @Autowired AnnotationRetrievingJobService subject;

  AnnotationRetrievingJob job() {
    return AnnotationRetrievingJob.builder()
        .id(JOB_ID)
        .annotationJobId(ANNOTATION_JOB_ID)
        .detectionJobId(DETECTION_JOB_ID)
        .statusHistory(List.of())
        .submissionInstant(null)
        .build();
  }

  @BeforeEach
  void create_job() {
    repository.save(job());
  }

  @AfterEach
  void delete_job() {
    repository.delete(job());
  }

  @Test
  void read_ok() {
    var actual1 = subject.findById(JOB_ID);
    var actual2 = subject.getByAnnotationJobId(ANNOTATION_JOB_ID);
    var actual3 = subject.findAllByDetectionJobId(DETECTION_JOB_ID);

    assertEquals(job(), actual1.toBuilder().submissionInstant(null).build());
    assertEquals(actual2, actual1);
    assertTrue(actual3.contains(actual1));
  }

  @Test
  void read_ko() {
    assertThrows(NotFoundException.class, () -> subject.findById("dummy"));
    assertThrows(NotFoundException.class, () -> subject.getByAnnotationJobId("dummy"));
  }
}
