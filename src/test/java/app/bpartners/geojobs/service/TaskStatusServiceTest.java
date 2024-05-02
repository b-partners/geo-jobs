package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;

class TaskStatusServiceTest {
  JpaRepository<TestJob, String> jobRepository = mock();
  JobStatusRepository jobStatusRepository = mock();
  EventProducer eventProducer = mock();
  TaskRepository<TestTask> taskRepository = mock();
  TaskStatusRepository taskStatusRepository = mock();
  JobService<TestTask, TestJob> jobService =
      new TestJobService(jobRepository, jobStatusRepository, taskRepository, eventProducer);
  TaskStatusService<TestTask, TestJob> subject =
      new TaskStatusService<>(taskStatusRepository, jobService);
  EntityManager em = mock();

  @BeforeEach
  void setUp() {
    jobService.setEm(em);
    when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
  }

  @Test
  void invoke_jobService_onStatusChange_when_status_changes() {
    var jobId = "jobId";
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(aTestJob(jobId, PENDING, UNKNOWN)));
    var taskId = "taskId";
    var oldTask = aTestTask(taskId, jobId, PENDING, UNKNOWN);
    when(taskRepository.findAllByJobId(jobId))
        .thenReturn(
            List.of(
                aTestTask(taskId, jobId, FINISHED, SUCCEEDED),
                aTestTask(taskId, jobId, FINISHED, FAILED)));

    subject.fail(oldTask);

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventsCaptor.capture());
    var statusChanged = (StatusChanged) eventsCaptor.getValue().get(0);
    assertEquals(PENDING, statusChanged.oldStatus.getProgression());
    assertEquals(UNKNOWN, statusChanged.oldStatus.getHealth());
    assertEquals(FINISHED, statusChanged.newStatus.getProgression());
    assertEquals(FAILED, statusChanged.newStatus.getHealth());
  }

  @Test
  void aTaskFails_then_job_fails_while_remaining_processing() {
    var jobId = "jobId";
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(aTestJob(jobId, PENDING, UNKNOWN)));
    var taskId = "taskId";
    var oldTask = aTestTask(taskId, jobId, PENDING, UNKNOWN);
    when(taskRepository.findAllByJobId(jobId))
        .thenReturn(
            List.of(
                aTestTask(taskId, jobId, PROCESSING, UNKNOWN),
                aTestTask(taskId, jobId, FINISHED, FAILED)));

    subject.fail(oldTask);

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventsCaptor.capture());
    var statusChanged = (StatusChanged) eventsCaptor.getValue().get(0);
    assertEquals(PENDING, statusChanged.oldStatus.getProgression());
    assertEquals(UNKNOWN, statusChanged.oldStatus.getHealth());
    assertEquals(PROCESSING, statusChanged.newStatus.getProgression());
    assertEquals(FAILED, statusChanged.newStatus.getHealth());
  }

  @Test
  void aTaskSucceeds_then_job_remains_processing_if_other_tasks_are_still_processing() {
    var jobId = "jobId";
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(aTestJob(jobId, PENDING, UNKNOWN)));
    var taskId = "taskId";
    var oldTask = aTestTask(taskId, jobId, PENDING, UNKNOWN);
    when(taskRepository.findAllByJobId(jobId))
        .thenReturn(
            List.of(
                aTestTask(taskId, jobId, PROCESSING, UNKNOWN),
                aTestTask(taskId, jobId, FINISHED, SUCCEEDED)));

    subject.fail(oldTask);

    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventsCaptor.capture());
    var statusChanged = (StatusChanged) eventsCaptor.getValue().get(0);
    assertEquals(PENDING, statusChanged.oldStatus.getProgression());
    assertEquals(UNKNOWN, statusChanged.oldStatus.getHealth());
    assertEquals(PROCESSING, statusChanged.newStatus.getProgression());
    assertEquals(UNKNOWN, statusChanged.newStatus.getHealth());
  }

  @Test
  void do_NOT_invoke_jobService_onStatusChange_when_status_does_NOT_changes() {
    var jobId = "jobId";
    when(jobRepository.findById(jobId))
        .thenReturn(Optional.of(aTestJob(jobId, PROCESSING, UNKNOWN)));
    var taskId = "taskId";
    var oldTask = aTestTask(taskId, jobId, PENDING, UNKNOWN);
    when(taskRepository.findAllByJobId(jobId))
        .thenReturn(
            List.of(
                aTestTask(taskId, jobId, FINISHED, SUCCEEDED),
                aTestTask(taskId, jobId, PROCESSING, UNKNOWN)));

    subject.process(oldTask);

    verify(eventProducer, times(0)).accept(any());
  }

  private static TestTask aTestTask(
      String taskId, String jobId, ProgressionStatus progression, HealthStatus health) {
    var task = new TestTask();
    task.setId(taskId);
    task.setJobId(jobId);
    var statusHistory = new ArrayList<TaskStatus>();
    statusHistory.add(
        TaskStatus.builder()
            .progression(progression)
            .health(health)
            .creationDatetime(now())
            .build());
    task.setStatusHistory(statusHistory);
    return task;
  }

  private static TestJob aTestJob(
      String jobId, ProgressionStatus progression, HealthStatus health) {
    var job = new TestJob();
    job.setId(jobId);
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(
        JobStatus.builder()
            .progression(progression)
            .health(health)
            .creationDatetime(now())
            .build());
    job.setStatusHistory(statusHistory);
    return job;
  }

  static class TestTask extends Task {
    @Override
    public JobType getJobType() {
      return null;
    }

    @Override
    public Task semanticClone() {
      return null;
    }
  }

  @SuperBuilder(toBuilder = true)
  @NoArgsConstructor
  static class TestJob extends Job {
    @Override
    protected JobType getType() {
      return null;
    }

    @Override
    public Job semanticClone() {
      return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
    }
  }

  static class TestJobService extends JobService<TestTask, TestJob> {
    public TestJobService(
        JpaRepository<TestJob, String> repository,
        JobStatusRepository jobStatusRepository,
        TaskRepository<TestTask> taskRepository,
        EventProducer eventProducer) {
      super(repository, jobStatusRepository, taskRepository, eventProducer, TestJob.class);
    }

    @Override
    protected void onStatusChanged(TestJob oldJob, TestJob newJob) {
      eventProducer.accept(List.of(new StatusChanged(oldJob.getStatus(), newJob.getStatus())));
    }
  }

  record StatusChanged(JobStatus oldStatus, JobStatus newStatus) {}
}
