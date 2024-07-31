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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    Optional<ZoneTilingJob> zoneTilingJobOpt = zoneTilingJobRepository.findById(ZTJId);
    Optional<ZoneDetectionJob> zoneDetectionJobOpt = zoneDetectionJobRepository.findById(ZDJId);

    if (zoneTilingJobOpt.isPresent() && zoneDetectionJobOpt.isPresent()) {
      ZoneTilingJob zoneTilingJob = zoneTilingJobOpt.get();
      ZoneDetectionJob zoneDetectionJob = zoneDetectionJobOpt.get();

      if (zoneDetectionJob.isPending()) {
        zoneTilingJob.setEndToEndId(endToEndId);
        zoneTilingJobRepository.save(zoneTilingJob);
        processZoneDetectionJob(zoneToDetect, zoneTilingJob);
        List<TaskStatistic> stat = getStatisticsFrom(zoneTilingJob.getId(), ZDJId);
        return createFullDetectedZone(fullDetection.getGeoJsonS3FileKey(), stat);
      }

      List<TaskStatistic> stat = getStatisticsFrom(zoneTilingJob.getId(), ZDJId);
      return createFullDetectedZone(fullDetection.getGeoJsonS3FileKey(), stat);
    }

    ZoneTilingJob zoneTilingJob = zoneTilingJobRepository.findByEndToEndId(endToEndId);
    if (zoneTilingJob != null) {
      List<TaskStatistic> stat = getStatisticsFrom(zoneTilingJob.getId(), ZDJId);
      return createFullDetectedZone(fullDetection.getGeoJsonS3FileKey(), stat);
    }

    ZoneTilingJob newJob = processZoneTilingJob(zoneToDetect);
    List<TaskStatistic> stat = getStatisticsFrom(newJob.getId(), ZDJId);
    return createFullDetectedZone(fullDetection.getGeoJsonS3FileKey(), stat);
  }

  private List<TaskStatistic> getStatisticsFrom(String ztjId, String zdjId) {
    ArrayList<TaskStatistic> statistics = new ArrayList<>();
    TaskStatistic ZTJStat = zoneTilingJobService.computeTaskStatistics(ztjId);
    TaskStatistic ZDJStat = zoneDetectionJobService.computeTaskStatistics(zdjId);
    statistics.add(ZTJStat);
    statistics.add(ZDJStat);
    return statistics;
  }

  private FullDetectedZone createFullDetectedZone(
      String geoJsonUrl, List<TaskStatistic> statistics) {
    return new FullDetectedZone()
        .jobTypes(List.of(TILING, MACHINE_DETECTION, HUMAN_DETECTION))
        .detectedGeojsonUrl(geoJsonUrl)
        .statistics(statistics.stream().map(taskStatisticMapper::toRest).toList());
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

  public void processZoneDetectionJob(CreateFullDetection zoneToDetect, ZoneTilingJob job) {
    String ZTJId = job.getId();
    ZoneDetectionJob zoneDetectionJob = zoneDetectionJobRepository.findByZoneTilingJobId(ZTJId);
    FullDetection fullDetection =
        fullDetectionRepository.findByEndToEndId(zoneToDetect.getEndToEndId());
    detectionjobValidator.accept(zoneDetectionJob.getId());
    DetectableObjectType detectableObjectType = zoneToDetect.getObjectType();
    if (detectableObjectType == null) {
      throw new ApiException(SERVER_EXCEPTION, "Object to detect is mandatory. ");
    }

    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        List.of(
            new DetectableObjectConfiguration()
                .type(detectableObjectType)
                .confidence(zoneToDetect.getConfidence()));
    fullDetection.setDetectableObjectConfiguration(detectableObjectConfigurations.getFirst());
    fullDetectionRepository.save(fullDetection);
    ZoneDetectionJob processedZDJ =
        zoneDetectionJobService.processZDJ(
            zoneDetectionJob.getId(), detectableObjectConfigurations);

    JobStatus jobStatus = processedZDJ.getStatus();
    if (!FINISHED.equals(jobStatus.getProgression()) && !SUCCEEDED.equals(jobStatus.getHealth())) {
      eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(processedZDJ.getId())));
    }
  }
}
