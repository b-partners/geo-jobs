package app.bpartners.geojobs.service;

<<<<<<< Updated upstream
import static app.bpartners.geojobs.endpoint.rest.model.JobType.DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.Type.HUMAN;
import static app.bpartners.geojobs.endpoint.rest.model.Type.MACHINE;
import static app.bpartners.geojobs.endpoint.rest.model.Type.TILING;
=======
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.HUMAN_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.TILING;
>>>>>>> Stashed changes
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.DetectedZone;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityZoneDetectionJobProcessAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityZoneTilingJobProcessAuthorizer;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;

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

  public DetectedZone processTilingAndDetection(CreateFullDetection zoneToDetect) {
    var createJob = zoneTilingJobMapper.from(zoneToDetect);
    communityZoneTilingJobProcessAuthorizer.accept(createJob);
    var job = zoneTilingJobMapper.toDomain(createJob);
    var tilingTasks = getTilingTasks(createJob, job.getId());
    ZoneTilingJob zoneTilingJob = zoneTilingJobService.create(job, tilingTasks);
    String jobId = zoneTilingJob.getId();
    jobValidator.accept(zoneTilingJob.getId());
    JobStatus ZTJStatus = zoneTilingJob.getStatus();
    if (!FINISHED.equals(ZTJStatus.getProgression())){
      eventProducer.accept(List.of(new ZTJStatusRecomputingSubmitted(jobId)));
    }

    List<DetectableObjectType> detectableObjects = zoneToDetect.getObjectType();
    if (detectableObjects == null){
      throw new ApiException(SERVER_EXCEPTION, "You should provide object to detect");
    }
    List<DetectableObjectConfiguration> detectableObjectConfigurations = detectableObjects
            .stream().map(detectableObjectType -> new DetectableObjectConfiguration().type(detectableObjectType)).toList();
    communityZoneDetectionJobProcessAuthorizer.accept(jobId, detectableObjectConfigurations);
    List<app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration>
        configurations =
            detectableObjectConfigurations.stream()
                .map(objectConf -> objectConfigurationMapper.toDomain(jobId, objectConf))
                .toList();
    ZoneDetectionJob processedZDJ = zoneDetectionJobService.fireTasks(jobId, configurations);
    JobStatus jobStatus = processedZDJ.getStatus();
    if (!jobStatus.getProgression().equals(FINISHED)) {
      eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(processedZDJ.getId())));
    }

    return new DetectedZone()
<<<<<<< Updated upstream
            .type(List.of(TILING, HUMAN, MACHINE))
            .geojsonUrl(zoneDetectionJobService.getGeoJsonsUrl(processedZDJ.getId()).toString());
=======
        .jobTypes(List.of(TILING, MACHINE_DETECTION, HUMAN_DETECTION))
        .statistics(Stream.of(ZTJStat, ZDJStat).map(taskStatisticMapper::toRest).toList())
        .detectedGeojsonUrl(zoneDetectionJobService.getGeoJsonsUrl(processedZDJ.getId()).toString());
>>>>>>> Stashed changes
  }
}
