package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.AnnotationRetrievingTaskService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class AnnotationRetrievingJobStatusRecomputingSubmittedServiceIT extends FacadeIT {
  @MockBean AnnotationRetrievingTaskService annotationRetrievingTaskService;
  @MockBean AnnotationRetrievingJobService annotationRetrievingJobService;
  @MockBean EventProducer<ZDJStatusRecomputingSubmitted> eventProducer;
  @Autowired AnnotationRetrievingJobStatusRecomputingSubmittedService subject;

  AnnotationRetrievingJobStatusRecomputingSubmitted submitted() {
    return AnnotationRetrievingJobStatusRecomputingSubmitted.builder()
        .annotationRetrievingJobId("jobId")
        .build();
  }

  ZDJStatusRecomputingSubmitted event() {
    return new ZDJStatusRecomputingSubmitted("detectionJobId");
  }

  @Test
  void accept_on_task_empty() {
    when(annotationRetrievingTaskService.getByRetrievingJobId(any())).thenReturn(List.of());

    subject.accept(submitted());

    verify(eventProducer, never()).accept(List.of(event()));
  }

  @Test
  void accept_on_task_succeed() {
    when(annotationRetrievingTaskService.getByRetrievingJobId(any()))
        .thenReturn(
            List.of(
                AnnotationRetrievingTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
                    .build()));
    when(annotationRetrievingJobService.getByAnnotationJobId(any()))
        .thenReturn(AnnotationRetrievingJob.builder().detectionJobId("detectionJobId").build());

    subject.accept(submitted());

    verify(eventProducer, times(1)).accept(List.of(event()));
  }

  @Test
  void accept_on_task_fail() {
    when(annotationRetrievingTaskService.getByRetrievingJobId(any()))
        .thenReturn(
            List.of(
                AnnotationRetrievingTask.builder()
                    .statusHistory(
                        List.of(TaskStatus.builder().progression(FINISHED).health(FAILED).build()))
                    .build()));
    when(annotationRetrievingJobService.getByAnnotationJobId(any()))
        .thenReturn(AnnotationRetrievingJob.builder().build());

    subject.accept(submitted());

    verify(eventProducer, never()).accept(List.of(event()));
  }
}
