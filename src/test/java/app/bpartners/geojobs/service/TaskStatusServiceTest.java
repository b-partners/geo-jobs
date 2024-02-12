package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.Job;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.JobType;
import app.bpartners.geojobs.repository.model.Status.HealthStatus;
import app.bpartners.geojobs.repository.model.Status.ProgressionStatus;
import app.bpartners.geojobs.repository.model.Task;
import app.bpartners.geojobs.repository.model.TaskStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.repository.JpaRepository;

class TaskStatusServiceTest {
  JpaRepository<TestJob, String> jobRepository = mock();
  EventProducer eventProducer = mock();
  TaskRepository<TestTask> taskRepository = mock();
  JobService<TestTask, TestJob> jobService =
      new TestJobService(jobRepository, taskRepository, eventProducer);
  TaskStatusService<TestTask, TestJob> subject =
      new TaskStatusService<>(taskRepository, jobService);

  @BeforeEach
  void setUp() {
    subject.setEm(mock());
    when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
  }

  @Test
  void invoke_jobService_onStatusChange_when_status_changes() {
    var taskId = "taskId";
    var jobId = "jobId";
    var oldTask = aTestTask(taskId, jobId, PENDING, UNKNOWN);
    when(taskRepository.existsById(taskId)).thenReturn(true);
    var oldJob = aTestJob(jobId, PENDING, UNKNOWN);
    var newJob = aTestJob(jobId, FINISHED, FAILED);
    when(jobRepository.findById(jobId))
        .thenReturn(Optional.of(oldJob))
        .thenReturn(Optional.of(newJob));

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
  void do_NOT_invoke_jobService_onStatusChange_when_status_does_NOT_changes() {
    var taskId = "taskId";
    var jobId = "jobId";
    var oldTask = aTestTask(taskId, jobId, PROCESSING, UNKNOWN);
    when(taskRepository.existsById(taskId)).thenReturn(true);
    var oldJob = aTestJob(jobId, PROCESSING, UNKNOWN);
    var newJob = aTestJob(jobId, PROCESSING, UNKNOWN);
    when(jobRepository.findById(jobId))
        .thenReturn(Optional.of(oldJob))
        .thenReturn(Optional.of(newJob));

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
  }

  static class TestJob extends Job {
    @Override
    protected JobType getType() {
      return null;
    }
  }

  static class TestJobService extends JobService<TestTask, TestJob> {
    public TestJobService(
        JpaRepository<TestJob, String> repository,
        TaskRepository<TestTask> taskRepository,
        EventProducer eventProducer) {
      super(repository, taskRepository, eventProducer);
    }

    @Override
    protected void onStatusChanged(TestJob oldJob, TestJob newJob) {
      eventProducer.accept(List.of(new StatusChanged(oldJob.getStatus(), newJob.getStatus())));
    }
  }

  record StatusChanged(JobStatus oldStatus, JobStatus newStatus) {}
}
