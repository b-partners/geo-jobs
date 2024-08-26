package app.bpartners.geojobs.service.event;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.COMPLETED;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AnnotationRetriever {
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final AnnotationService annotationService;
  private final AnnotationRetrievingJobService annotationRetrievingJobService;

  public void accept(String humanZDJId) {
    var humanDetectionJobs = humanDetectionJobRepository.findAllByZoneDetectionJobId(humanZDJId);
    if (humanDetectionJobs.isEmpty()) {
      return;
    }
    var annotationJobs =
        humanDetectionJobs.stream()
            .map(
                humanDetectionJob ->
                    annotationService.getAnnotationJobById(humanDetectionJob.getAnnotationJobId()))
            .toList();
    if (annotationJobs.isEmpty()) {
      log.warn(
          "Annotation jobs from annotator is empty, although humanDetectionJobs not empty {}",
          humanDetectionJobs);
      return;
    }
    if (annotationJobs.stream().allMatch(job -> COMPLETED.equals(job.getStatus()))) {
      List<RetrievingJobWithTasks> retrievingJobsWithTasks =
          annotationJobs.stream()
              .map(
                  aJob -> {
                    var retrievingJob =
                        AnnotationRetrievingJob.builder()
                            .id(randomUUID().toString())
                            .annotationJobId(aJob.getId())
                            .detectionJobId(humanZDJId)
                            .statusHistory(List.of())
                            .build();
                    var retrievingTasks =
                        annotationService.retrieveTasksFromAnnotationJob(
                            humanZDJId,
                            retrievingJob.getId(),
                            retrievingJob.getAnnotationJobId(),
                            null,
                            null,
                            null);
                    return new RetrievingJobWithTasks(retrievingJob, retrievingTasks);
                  })
              .toList();

      retrievingJobsWithTasks.forEach(
          record -> {
            var job = record.job;
            var tasks = record.tasks;
            AnnotationRetrievingJob newJob = annotationRetrievingJobService.create(job, tasks);

            // TODO: set into AnnotationRetrievingJobCreated
            annotationRetrievingJobService.fireTasks(newJob.getId());
          });
    }
  }

  record RetrievingJobWithTasks(
      AnnotationRetrievingJob job, List<AnnotationRetrievingTask> tasks) {}
}
