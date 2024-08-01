package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ZDJStatusRecomputingSubmittedService
    implements Consumer<ZDJStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ZoneDetectionJob, ParcelDetectionTask, ZDJStatusRecomputingSubmitted>
      service;
  private final AnnotationRetrievingJobService annotationRetrievingJobService;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final GeoJsonConversionInitiationService geoJsonConversionInitiationService;
  private final JobAnnotationService jobAnnotationService;

  public ZDJStatusRecomputingSubmittedService(
      ZoneDetectionJobService jobService,
      EventProducer eventProducer,
      TaskStatusService<ParcelDetectionTask> taskStatusService,
      TaskRepository<ParcelDetectionTask> taskRepository,
      AnnotationRetrievingJobService annotationRetrievingJobService,
      GeoJsonConversionInitiationService geoJsonConversionInitiationService) {
      ZoneDetectionJobService zoneDetectionJobService,
      JobAnnotationService jobAnnotationService) {
    this.jobAnnotationService = jobAnnotationService;
    this.service =
        new JobStatusRecomputingSubmittedService<>(
            eventProducer, jobService, taskStatusService, taskRepository);
    this.zoneDetectionJobService = jobService;
    this.annotationRetrievingJobService = annotationRetrievingJobService;
    this.geoJsonConversionInitiationService = geoJsonConversionInitiationService;
  }

  @Override
  public void accept(ZDJStatusRecomputingSubmitted event) {
    var detectionJobId = event.getJobId();
    var annotationRetrievingJob =
        annotationRetrievingJobService.getByDetectionJobId(detectionJobId);
    if (annotationRetrievingJob.isEmpty()
        || !annotationRetrievingJob.stream().allMatch(AnnotationRetrievingJob::isSucceeded)) {
      service.accept(event);
      return;
    }
    var humanZDJ = zoneDetectionJobService.getHumanZdjFromZdjId(detectionJobId);
    humanZDJ.hasNewStatus(
        Status.builder()
            .id(randomUUID().toString())
            .progression(FINISHED)
            .health(SUCCEEDED)
            .creationDatetime(now())
            .build());
    geoJsonConversionInitiationService.processConversionTask(
        humanZDJ.getZoneName(), humanZDJ.getId());
    service.accept(event);
    ZoneDetectionJob zoneDetectionJob = zoneDetectionJobService.findById(event.getJobId());
    if (zoneDetectionJob.isFinished() && zoneDetectionJob.isSucceeded()) {
      jobAnnotationService.processAnnotationJob(event.getJobId(), Double.valueOf(0.8));
    }
  }
}
