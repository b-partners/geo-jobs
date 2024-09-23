package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStep.*;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
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
  private final ZoneDetectionJobValidator detectionJobValidator;
  private final EventProducer eventProducer;
  private final DetectionStepStatisticMapper detectionStepStatisticMapper;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final DetectionRepository detectionRepository;
  private final ZoneTilingJobRepository zoneTilingJobRepository;
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final BucketComponent bucketComponent;
  private final GeoJsonConversionInitiationService conversionInitiationService;
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;

  private List<Feature> readFromFile(File featuresFromShape) {
    return List.of(new Feature().id("TODO: read features from shape"));
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection finalizeShapeConfig(
      String detectionId, File featuresFromShape) {
    var detection =
        detectionRepository
            .findById(detectionId)
            .orElseThrow(
                () -> new NotFoundException("Detection(id=" + detectionId + ") not found"));
    var savedDetection =
        detectionRepository.save(
            detection.toBuilder().geoJsonZone(readFromFile(featuresFromShape)).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeFromConfiguring(savedDetection);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection configureShapeFile(
      String detectionId, File shapeFile) {
    var detection =
        detectionRepository
            .findById(detectionId)
            .orElseThrow(
                () -> new NotFoundException("Detection(id=" + detectionId + ") not found"));
    var bucketKey = "detections/shape/" + detectionId;
    bucketComponent.upload(shapeFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().shapeFileKey(bucketKey).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeFromConfiguring(savedDetection);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection processDetection(
      String detectionId, CreateDetection zoneToDetect, Optional<String> communityOwnerId) {
    var detection =
        detectionRepository
            .findByEndToEndId(detectionId)
            .orElseGet(
                () -> {
                  var detectableObjectConfigurations =
                      detectableObjectTypeMapper.mapDefaultConfigurationsFromModel(
                          detectionId,
                          Objects.requireNonNull(zoneToDetect.getDetectableObjectConfiguration())
                              .getActualInstance());
                  Detection toSave =
                      Detection.builder()
                          .id(detectionId)
                          .endToEndId(detectionId)
                          .communityOwnerId(communityOwnerId.orElse(null))
                          .detectableObjectConfigurations(detectableObjectConfigurations)
                          .detectionOverallConfiguration(zoneToDetect.getOverallConfiguration())
                          .geoJsonZone(zoneToDetect.getGeoJsonZone())
                          .build();
                  var savedFullDetection =
                      communityUsedSurfaceService.persistFullDetectionWithSurfaceUsage(
                          toSave, zoneToDetect.getGeoJsonZone());
                  eventProducer.accept(
                      List.of(DetectionSaved.builder().detection(savedFullDetection).build()));
                  return savedFullDetection;
                });

    if (detection.getGeoJsonZone() == null || detection.getGeoJsonZone().isEmpty()) {
      return computeFromConfiguring(detection);
    }

    if (detection.getZtjId() == null) {
      var ztj = processZoneTilingJob(zoneToDetect);
      var detectionWithZTJ =
          detectionRepository.save(detection.toBuilder().ztjId(ztj.getId()).build());
      return getTilingStatistics(detectionWithZTJ, ztj.getId());
    }

    var tilingJobId = detection.getZtjId();
    var detectionJobId = detection.getZdjId();
    var zoneTilingJob = zoneTilingJobRepository.findById(tilingJobId).orElse(null);
    var machineZoneDetectionJob =
        detectionJobId == null
            ? null
            : zoneDetectionJobRepository.findById(detectionJobId).orElse(null);

    assert zoneTilingJob != null;
    if (!zoneTilingJob.isSucceeded()) {
      return getTilingStatistics(detection, tilingJobId);
    }

    assert machineZoneDetectionJob != null;
    if (machineZoneDetectionJob.isPending() && zoneTilingJob.isFinished()) {
      var savedDetectionJob = processZoneDetectionJob(detection, zoneTilingJob);
      return getDetectionStatistics(detection, savedDetectionJob.getId());
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
            detection, humanZoneDetectionJob.getZoneName(), humanZoneDetectionJob.getId());
        // TODO: return human zone detection job statistics
      }
    }
    return getDetectionStatistics(detection, detectionJobId);
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Detection> getDetectionsByCriteria(
      Optional<String> communityId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    var detections =
        communityId
            .map(ownerId -> detectionRepository.findByCommunityOwnerId(ownerId, pageable))
            .orElseGet(() -> detectionRepository.findAll(pageable).getContent());
    return detections.stream().map(this::addStatistics).toList();
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection addStatistics(Detection detection) {
    DetectionStep detectionStep = TILING;
    TaskStatistic statistic;
    if (detection.getZdjId() != null) {
      statistic = zoneDetectionJobService.getTaskStatistic(detection.getZdjId());
      detectionStep = MACHINE_DETECTION;
    } else if (detection.getZtjId() != null) {
      statistic = zoneTilingJobService.getTaskStatistic(detection.getZtjId());
    } else {
      throw new ApiException(
          SERVER_EXCEPTION, "Unknown supported step for detection (id=" + detection.getId() + ")");
    }

    return createDetection(detection, statistic, detectionStep);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection computeFromConfiguring(
      Detection detection) {
    var defaultConfiguringStatistic =
        TaskStatistic.builder()
            .jobType(GeoJobType.CONFIGURING)
            .actualJobStatus(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .creationDatetime(now())
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .jobType(GeoJobType.CONFIGURING)
                    .build())
            .taskStatusStatistics(List.of())
            .build();
    return createDetection(detection, defaultConfiguringStatistic, CONFIGURING);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection getTilingStatistics(
      Detection detection, String tilingJobId) {
    return createDetection(
        detection, zoneTilingJobService.computeTaskStatistics(tilingJobId), TILING);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection getDetectionStatistics(
      Detection detection, String detectionJobId) {
    return createDetection(
        detection,
        zoneDetectionJobService.computeTaskStatistics(detectionJobId),
        MACHINE_DETECTION);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection createDetection(
      Detection detection, TaskStatistic statistic, DetectionStep detectionStep) {
    return new app.bpartners.geojobs.endpoint.rest.model.Detection()
        .id(detection.getEndToEndId())
        .excelUrl(null) // TODO: not handle for now
        .shapeUrl(generatePresignedUrl(detection.getShapeFileKey()))
        .geoJsonZone(detection.getGeoJsonZone())
        .geoJsonUrl(generatePresignedUrl(detection.getGeojsonS3FileKey()))
        .overallConfiguration(detection.getDetectionOverallConfiguration())
        .step(detectionStepStatisticMapper.toRestDetectionStepStatus(statistic, detectionStep));
  }

  private ZoneTilingJob processZoneTilingJob(CreateDetection zoneToDetect) {
    var createJob = zoneTilingJobMapper.from(zoneToDetect);
    var job = zoneTilingJobMapper.toDomain(createJob);
    var tilingTasks = getTilingTasks(createJob, job.getId());

    return zoneTilingJobService.create(job, tilingTasks);
  }

  // TODO: seems to be bad to handle FullDetection and CreateDetection together
  public ZoneDetectionJob processZoneDetectionJob(Detection detection, ZoneTilingJob job) {
    var zoneDetectionJob = zoneDetectionJobService.getByTilingJobId(job.getId(), MACHINE);

    detectionJobValidator.accept(zoneDetectionJob.getId());

    return zoneDetectionJobService.processZDJ(
        zoneDetectionJob.getId(), detection.getDetectableObjectConfigurations());
  }

  // TODO: set in S3
  private String generatePresignedUrl(String fileKey) {
    if (fileKey == null) {
      return null;
    }
    return bucketComponent.presign(fileKey, PRE_SIGNED_URL_DURATION).toString();
  }
}
