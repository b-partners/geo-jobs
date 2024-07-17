package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.HUMAN_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.TILING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.DetectedZone;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityZoneDetectionJobProcessAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityZoneTilingJobProcessAuthorizer;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
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
  private final CommunityZoneDetectionJobProcessAuthorizer
      communityZoneDetectionJobProcessAuthorizer;
  private final CommunityZoneTilingJobProcessAuthorizer communityZoneTilingJobProcessAuthorizer;
  private final ZoneDetectionJobValidator jobValidator;
  private final DetectableObjectConfigurationMapper objectConfigurationMapper;
  private final EventProducer eventProducer;
  private final TaskStatisticMapper taskStatisticMapper;

  public DetectedZone processTilingAndDetection(CreateFullDetection zoneToDetect) {
    var createJob = zoneTilingJobMapper.from(zoneToDetect);
    communityZoneTilingJobProcessAuthorizer.accept(createJob);
    var job = zoneTilingJobMapper.toDomain(createJob);
    var tilingTasks = getTilingTasks(createJob, job.getId());
    ZoneTilingJob zoneTilingJob = zoneTilingJobService.create(job, tilingTasks);
    String ZTJId = zoneTilingJob.getId();
    jobValidator.accept(zoneTilingJob.getId());
    JobStatus ZTJStatus = zoneTilingJob.getStatus();
    if (!FINISHED.equals(ZTJStatus.getProgression())) {
      eventProducer.accept(List.of(new ZTJStatusRecomputingSubmitted(ZTJId)));
    }

    DetectableObjectType detectableObjectType = zoneToDetect.getObjectType();
    if (detectableObjectType == null) {
      throw new ApiException(SERVER_EXCEPTION, "You should provide object to detect");
    }
    List<DetectableObjectConfiguration> detectableObjectConfigurations = List.of
            (new DetectableObjectConfiguration().type(detectableObjectType));
    communityZoneDetectionJobProcessAuthorizer.accept(ZTJId, detectableObjectConfigurations);
    List<app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration>
        configurations =
            detectableObjectConfigurations.stream()
                .map(objectConf -> objectConfigurationMapper.toDomain(ZTJId, objectConf))
                .toList();
    ZoneDetectionJob processedZDJ = zoneDetectionJobService.fireTasks(ZTJId, configurations);
    JobStatus jobStatus = processedZDJ.getStatus();
    String ZDJId = processedZDJ.getId();
    if (!jobStatus.getProgression().equals(FINISHED)) {
      eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(processedZDJ.getId())));
    }

    TaskStatistic ZTJStat = zoneTilingJobService.computeTaskStatistics(ZTJId);
    TaskStatistic ZDJStat = zoneDetectionJobService.computeTaskStatistics(ZDJId);

    return new DetectedZone()
        .jobTypes(List.of(TILING, MACHINE_DETECTION, HUMAN_DETECTION))
        .detectedGeojsonUrl(zoneDetectionJobService.getGeoJsonsUrl(processedZDJ.getId()).toString())
        .statistics(Stream.of(ZTJStat, ZDJStat).map(taskStatisticMapper::toRest).toList());
  }
}
