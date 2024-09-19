package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStep.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStep.TILING;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FullDetectionSaved;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStep;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneService {
  private static final Duration PRE_SIGNED_URL_DURATION = Duration.ofHours(1L);
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneTilingJobService zoneTilingJobService;
  private final ZoneTilingJobMapper zoneTilingJobMapper;
  private final ZoneDetectionJobValidator detectionjobValidator;
  private final EventProducer eventProducer;
  private final TaskStatisticMapper taskStatisticMapper;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final FullDetectionRepository fullDetectionRepository;
  private final ZoneTilingJobRepository zoneTilingJobRepository;
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final BucketComponent bucketComponent;
  private final GeoJsonConversionInitiationService conversionInitiationService;

  public Detection processTilingAndDetection(
      String detectionId, CreateDetection zoneToDetect, Optional<String> communityOwnerId) {
    var endToEndId = detectionId;
    var fullDetection =
        fullDetectionRepository
            .findByEndToEndId(endToEndId)
            .orElseGet(
                () -> {
                  FullDetection toSave =
                      FullDetection.builder()
                          .id(randomUUID().toString())
                          .endToEndId(endToEndId)
                          .communityOwnerId(communityOwnerId.orElse(null))
                          .detectableObjectConfiguration(
                              new DetectableObjectConfiguration()) // TODO
                          .build();
                  var savedFullDetection =
                      communityUsedSurfaceService.persistFullDetectionWithSurfaceUsage(
                          toSave, zoneToDetect.getGeoJsonZone());
                  eventProducer.accept(
                      List.of(
                          FullDetectionSaved.builder().fullDetection(savedFullDetection).build()));
                  return savedFullDetection;
                });

    if (fullDetection.getZtjId() == null) {
      var ztj = processZoneTilingJob(zoneToDetect);
      var fullDetectionWithZTJ =
          fullDetectionRepository.save(fullDetection.toBuilder().ztjId(ztj.getId()).build());
      return getTilingStatistics(fullDetectionWithZTJ, ztj.getId());
    }

    var tilingJobId = fullDetection.getZtjId();
    var detectionJobId = fullDetection.getZdjId();
    var zoneTilingJob = zoneTilingJobRepository.findById(tilingJobId).orElse(null);
    var machineZoneDetectionJob =
        detectionJobId == null
            ? null
            : zoneDetectionJobRepository.findById(detectionJobId).orElse(null);

    assert zoneTilingJob != null;
    if (!zoneTilingJob.isSucceeded()) {
      return getTilingStatistics(fullDetection, tilingJobId);
    }

    assert machineZoneDetectionJob != null;
    if (machineZoneDetectionJob.isPending() && zoneTilingJob.isFinished()) {
      var savedDetectionJob = processZoneDetectionJob(fullDetection, zoneTilingJob);
      return getDetectionStatistics(fullDetection, savedDetectionJob.getId());
    }

    if (machineZoneDetectionJob.isFinished()) {
      var humanZoneDetectionJob = zoneDetectionJobService.getByTilingJobId(tilingJobId, HUMAN);
      if (!humanZoneDetectionJob.isFinished()) {
        eventProducer.accept(
            List.of(
                AnnotationJobVerificationSent.builder()
                    .humanZdjId(humanZoneDetectionJob.getId())
                    .build()));
        // TODO: return human zone detection job statistics
      } else {
        conversionInitiationService.processConversionTask(
            fullDetection, humanZoneDetectionJob.getZoneName(), humanZoneDetectionJob.getId());
        // TODO: return human zone detection job statistics
      }
    }
    return getDetectionStatistics(fullDetection, detectionJobId);
  }

  public List<Detection> getFullDetectionsByCriteria(
      Optional<String> communityId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    var fullDetections =
        communityId
            .map(ownerId -> fullDetectionRepository.findByCommunityOwnerId(ownerId, pageable))
            .orElseGet(() -> fullDetectionRepository.findAll(pageable).getContent());
    return fullDetections.stream().map(this::addStatistics).toList();
  }

  private Detection addStatistics(FullDetection fullDetection) {
    var jobType = TILING;
    List<TaskStatistic> statistics = new ArrayList<>();

    if (fullDetection.getZdjId() != null) {
      statistics.add(zoneDetectionJobService.getTaskStatistic(fullDetection.getZdjId()));
      jobType = MACHINE_DETECTION;
    } else if (fullDetection.getZtjId() != null) {
      statistics.add(zoneTilingJobService.getTaskStatistic(fullDetection.getZtjId()));
    }

    return createDetection(
        fullDetection.getEndToEndId(), fullDetection.getGeojsonS3FileKey(), statistics, jobType);
  }

  private Detection getTilingStatistics(FullDetection fullDetection, String tilingJobId) {
    return createDetection(
        fullDetection.getEndToEndId(),
        fullDetection.getGeojsonS3FileKey(),
        List.of(zoneTilingJobService.computeTaskStatistics(tilingJobId)),
        TILING);
  }

  private Detection getDetectionStatistics(FullDetection fullDetection, String detectionJobId) {
    return createDetection(
        fullDetection.getEndToEndId(),
        fullDetection.getGeojsonS3FileKey(),
        List.of(zoneDetectionJobService.computeTaskStatistics(detectionJobId)),
        MACHINE_DETECTION);
  }

  private Detection createDetection(
      String endToEndId, String s3FileKey, List<TaskStatistic> statistics, DetectionStep jobType) {
    Detection detection =
        new Detection()
            .id(endToEndId)
            .excelUrl(null) // TODO: not handle for now
            .shapeUrl(null) // TODO: not handle for now
            .geoJsonUrl(generatePresignedUrl(s3FileKey));
    // TODO: return overall configuration
    // TODO .step(statistics.stream().map(taskStatisticMapper::toRest).toList());
    return detection;
  }

  private ZoneTilingJob processZoneTilingJob(CreateDetection zoneToDetect) {
    var createJob = zoneTilingJobMapper.from(zoneToDetect);
    var job = zoneTilingJobMapper.toDomain(createJob);
    var tilingTasks = getTilingTasks(createJob, job.getId());

    return zoneTilingJobService.create(job, tilingTasks);
  }

  // TODO: seems to be bad to handle FullDetection and CreateDetection together
  public ZoneDetectionJob processZoneDetectionJob(FullDetection fullDetection, ZoneTilingJob job) {
    var zoneDetectionJob = zoneDetectionJobService.getByTilingJobId(job.getId(), MACHINE);

    detectionjobValidator.accept(zoneDetectionJob.getId());

    return zoneDetectionJobService.processZDJ(
        zoneDetectionJob.getId(), List.of(fullDetection.getDetectableObjectConfiguration()));
  }

  // TODO: set in S3
  private String generatePresignedUrl(String fileKey) {
    if (fileKey == null) {
      return null;
    }
    return bucketComponent.presign(fileKey, PRE_SIGNED_URL_DURATION).toString();
  }
}
