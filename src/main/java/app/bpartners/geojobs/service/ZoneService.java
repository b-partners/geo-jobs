package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.HUMAN_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.TILING;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.FullDetectedZone;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityZoneTilingJobProcessAuthorizer;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneService {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneTilingJobService zoneTilingJobService;
  private final ZoneTilingJobMapper zoneTilingJobMapper;
  private final CommunityZoneTilingJobProcessAuthorizer communityZoneTilingJobProcessAuthorizer;
  private final ZoneDetectionJobValidator detectionjobValidator;
  private final EventProducer eventProducer;
  private final TaskStatisticMapper taskStatisticMapper;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final FullDetectionRepository fullDetectionRepository;
  private final ZoneTilingJobRepository zoneTilingJobRepository;

  public FullDetectedZone processTilingAndDetection(CreateFullDetection zoneToDetect) {
    String endToEndId = zoneToDetect.getEndToEndId();
    FullDetection fullDetection = fullDetectionRepository.findByEndToEndId(endToEndId);
    String ZTJId = fullDetection.getZTJId();
    String ZDJId = fullDetection.getZDJId();
    var zoneTilingJob = zoneTilingJobRepository.findById(ZTJId);
    var zoneDetectionJob = zoneDetectionJobRepository.findById(ZDJId);

    if (zoneTilingJob.isPresent() && zoneDetectionJob.isPresent()) {
      processZoneDetectionJobs(zoneToDetect, zoneTilingJob.get());
    }

    ZoneTilingJob job = processZoneTilingJob(zoneToDetect);
    //    TODO: compute task statistics based on actual processing job
    TaskStatistic ZTJStat = zoneTilingJobService.computeTaskStatistics(job.getId());
    TaskStatistic ZDJStat = zoneDetectionJobService.computeTaskStatistics(ZDJId);

    return new FullDetectedZone()
        .jobTypes(List.of(TILING, MACHINE_DETECTION, HUMAN_DETECTION))
        //
        // .detectedGeojsonUrl(zoneDetectionJobService.getGeoJsonsUrl(job.getId()).toString())
        .statistics(
            Stream.concat(Stream.of(ZTJStat), Stream.of(ZDJStat))
                .map(stat -> taskStatisticMapper.toRest(stat))
                .toList());
  }

  private ZoneTilingJob processZoneTilingJob(CreateFullDetection zoneToDetect) {
    var createJob = zoneTilingJobMapper.from(zoneToDetect);
    communityZoneTilingJobProcessAuthorizer.accept(createJob);
    var job = zoneTilingJobMapper.toDomain(createJob);
    String ZTJId = job.getId();
    var tilingTasks = getTilingTasks(createJob, ZTJId);
    ZoneTilingJob zoneTilingJob = zoneTilingJobService.create(job, tilingTasks, zoneToDetect);
    return zoneTilingJob;
  }

  public void processZoneDetectionJobs(CreateFullDetection zoneToDetect, ZoneTilingJob job) {
    String ZTJId = job.getId();
    ZoneDetectionJob zoneDetectionJob = zoneDetectionJobRepository.findByZoneTilingJobId(ZTJId);
    detectionjobValidator.accept(zoneDetectionJob.getId());
    DetectableObjectType detectableObjectType = zoneToDetect.getObjectType();
    if (detectableObjectType == null) {
      throw new ApiException(SERVER_EXCEPTION, "Object to detect is mandatory. ");
    }

    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        List.of(new DetectableObjectConfiguration().type(detectableObjectType));
    ZoneDetectionJob processedZDJ =
        zoneDetectionJobService.processZDJ(
            zoneDetectionJob.getId(), detectableObjectConfigurations);

    JobStatus jobStatus = processedZDJ.getStatus();
    if (!FINISHED.equals(jobStatus.getProgression()) && !SUCCEEDED.equals(jobStatus.getHealth())) {
      eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(processedZDJ.getId())));
    }
  }
}
