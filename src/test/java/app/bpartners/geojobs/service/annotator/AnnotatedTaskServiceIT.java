package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.AnnotatedTaskRepository;
import app.bpartners.geojobs.repository.model.annotator.AnnotatedTask;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AnnotatedTaskServiceIT extends FacadeIT {
  private static final String MOCK_JOB_ID = "job_id";
  private static final String MOCK_CREATE_ANNOTATED_TASK_ID = "create_annotated_task_id";
  @Autowired private AnnotatedTaskRepository repository;
  @Autowired private AnnotatedTaskService subject;

  @Test
  void crupdate_then_read_annotated_task() {
    String taskId = randomUUID().toString();
    AnnotatedTask toSave =
        AnnotatedTask.builder()
            .id(taskId)
            .jobId(MOCK_JOB_ID)
            .createAnnotatedTaskId(MOCK_CREATE_ANNOTATED_TASK_ID)
            .asJobId(MOCK_JOB_ID)
            .jobType(DETECTION)
            .submissionInstant(null)
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(taskId)
                        .health(UNKNOWN)
                        .progression(Status.ProgressionStatus.PENDING)
                        .creationDatetime(now())
                        .build()))
            .build();

    var actual = subject.saveAll(List.of(toSave));
    var readed = subject.getByCreateAnnotatedTaskId(actual.getFirst().getCreateAnnotatedTaskId());

    assertEquals(1, actual.size());
    assertEquals(readed, actual.getFirst());
    assertFalse(readed.getStatusHistory().isEmpty());
  }
}
