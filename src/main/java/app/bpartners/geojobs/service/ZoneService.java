package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.CONFIGURING;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.TILING;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;
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
import app.bpartners.geojobs.endpoint.rest.model.BPLomModel;
import app.bpartners.geojobs.endpoint.rest.model.BPToitureModel;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
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
import app.bpartners.geojobs.service.detection.DetectionGeoJsonUpdateValidator;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.NonNull;
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
  private final ObjectMapper objectMapper;
  private final DetectionUpdateValidator detectionUpdateValidator;
  private final AuthProvider authProvider;
  private final DetectionGeoJsonUpdateValidator detectionGeoJsonUpdateValidator;

  private List<Feature> readFromFile(File featuresFromShape) {
    try {
      var featuresFileContent = Files.readString(featuresFromShape.toPath());
      return objectMapper.readValue(featuresFileContent, new TypeReference<>() {});
    } catch (Exception e) {
      throw new ApiException(
          CLIENT_EXCEPTION, "Unable to convert uploaded file to Features, exception=" + e);
    }
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection finalizeGeoJsonConfig(
      String detectionId, File featuresFromShape) {
    var detection = getDetectionById(detectionId);
    if (detection.getGeoJsonZone() != null && !detection.getGeoJsonZone().isEmpty()) {
      throw new BadRequestException(
          "Unable to finalize Detection(id=" + detectionId + ") geoJson as it already has values");
    }
    var savedDetection =
        detectionRepository.save(
            detection.toBuilder().geoJsonZone(readFromFile(featuresFromShape)).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeFromConfiguring(savedDetection, FINISHED, SUCCEEDED);
  }

  private Detection getDetectionById(String detectionId) {
    return detectionRepository
        .findById(detectionId)
        .orElseThrow(() -> new NotFoundException("Detection(id=" + detectionId + ") not found"));
  }

  private Detection getDetectionByE2eId(String detectionId) {
    return detectionRepository
        .findByEndToEndId(detectionId)
        .orElseThrow(
            () -> new NotFoundException("Detection(e2e.id=" + detectionId + ") not found"));
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection configureExcelFile(
      String detectionId, File excelFile) {
    var detection = getDetectionByE2eId(detectionId);
    detectionGeoJsonUpdateValidator.accept(detection);
    var bucketKey = "detections/excel/" + detectionId;
    bucketComponent.upload(excelFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().excelFileKey(bucketKey).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeFromConfiguring(savedDetection, PROCESSING, UNKNOWN);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection configureShapeFile(
      String detectionId, File shapeFile) {
    var detection = getDetectionByE2eId(detectionId);
    detectionGeoJsonUpdateValidator.accept(detection);
    var bucketKey = "detections/shape/" + detectionId;
    bucketComponent.upload(shapeFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().shapeFileKey(bucketKey).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeFromConfiguring(savedDetection, PROCESSING, UNKNOWN);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection processDetection(
      String e2Id, CreateDetection createDetection, Optional<String> optionalCommunityId) {
    var detection = getOrCreateDetection(e2Id, createDetection, optionalCommunityId);
    detectionUpdateValidator.accept(detection, createDetection);
    if (detection.getGeoJsonZone() == null || detection.getGeoJsonZone().isEmpty()) {
      return computeFromConfiguring(detection, PENDING, UNKNOWN);
    } else {
      if (!ROLE_ADMIN.equals(authProvider.getPrincipal().getRole())) {
        return computeFromConfiguring(detection, FINISHED, SUCCEEDED);
      }
    }
    if (detection.getZtjId() == null) {
      var ztj = processZoneTilingJob(detection);
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

  private Detection getOrCreateDetection(
      String endToEndId, CreateDetection createDetection, Optional<String> optionalCommunityId) {
    if (optionalCommunityId.isPresent()) {
      return detectionRepository
          .findByEndToEndIdAndCommunityOwnerId(endToEndId, optionalCommunityId.get())
          .orElseGet(saveDetection(endToEndId, createDetection, optionalCommunityId.get()));
    }
    return detectionRepository
        .findByEndToEndId(endToEndId)
        .orElseGet(saveDetection(endToEndId, createDetection, null));
  }

  @NonNull
  private Supplier<Detection> saveDetection(
      String endToEndId, CreateDetection createDetection, String communityOwnerId) {
    return () -> {
      var detectionToSave =
          mapFromRestCreateDetection(endToEndId, createDetection, communityOwnerId);
      var savedDetection =
          communityUsedSurfaceService.persistDetectionWithSurfaceUsage(
              detectionToSave, createDetection.getGeoJsonZone());
      eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
      return savedDetection;
    };
  }

  private Detection mapFromRestCreateDetection(
      String endToEndId, CreateDetection createDetection, @Nullable String communityOwnerId) {
    var detectableObjectModel = createDetection.getDetectableObjectModel();
    var modelActualInstance = Objects.requireNonNull(detectableObjectModel).getActualInstance();
    var detectionId = randomUUID().toString();
    var detectableObjectConfigurations =
        detectableObjectTypeMapper.mapDefaultConfigurationsFromModel(
            detectionId, modelActualInstance);
    var detectionBuilder =
        Detection.builder()
            .id(detectionId)
            .endToEndId(endToEndId)
            .emailReceiver(createDetection.getEmailReceiver())
            .zoneName(createDetection.getZoneName())
            .communityOwnerId(communityOwnerId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .geoServerProperties(createDetection.getGeoServerProperties())
            .geoJsonZone(createDetection.getGeoJsonZone());
    if (modelActualInstance instanceof BPToitureModel) {
      detectionBuilder.bpToitureModel((BPToitureModel) modelActualInstance);
    } else if (modelActualInstance instanceof BPLomModel) {
      detectionBuilder.bpLomModel((BPLomModel) modelActualInstance);
    }
    return detectionBuilder.build();
  }

  public List<app.bpartners.geojobs.endpoint.rest.model.Detection> getDetectionsByCriteria(
      Optional<String> communityId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    var detections =
        communityId
            .map(ownerId -> detectionRepository.findByCommunityOwnerId(ownerId, pageable))
            .orElseGet(() -> detectionRepository.findAll(pageable).getContent());

    for (var detection : detections) {
      detection.setId(detection.getEndToEndId());
    }
    return detections.stream().map(this::addStatistics).toList();
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection addStatistics(Detection detection) {
    if (detection.getZdjId() != null) {
      return createDetection(
          detection,
          zoneDetectionJobService.getTaskStatistic(detection.getZdjId()),
          MACHINE_DETECTION);
    }

    if (detection.getZtjId() != null) {
      return createDetection(
          detection, zoneTilingJobService.getTaskStatistic(detection.getZtjId()), TILING);
    }

    return createDetection(detection, new TaskStatistic(), CONFIGURING);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection computeFromConfiguring(
      Detection detection,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    var defaultConfiguringStatistic =
        TaskStatistic.builder()
            .jobType(GeoJobType.CONFIGURING)
            .actualJobStatus(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .creationDatetime(now())
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .jobType(GeoJobType.CONFIGURING)
                    .build())
            .updatedAt(now())
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
      Detection detection, TaskStatistic statistic, DetectionStepName detectionStepName) {
    return new app.bpartners.geojobs.endpoint.rest.model.Detection()
        .id(detection.getEndToEndId())
        .emailReceiver(detection.getEmailReceiver())
        .zoneName(detection.getZoneName())
        .excelUrl(generatePresignedUrl(detection.getExcelFileKey()))
        .shapeUrl(generatePresignedUrl(detection.getShapeFileKey()))
        .geoJsonZone(detection.getGeoJsonZone())
        .geoJsonUrl(generatePresignedUrl(detection.getGeojsonS3FileKey()))
        .geoServerProperties(detection.getGeoServerProperties())
        .detectableObjectModel(detection.getDetectableObjectModel())
        .step(detectionStepStatisticMapper.toRestDetectionStepStatus(statistic, detectionStepName));
  }

  private ZoneTilingJob processZoneTilingJob(Detection detection) {
    var createJob = zoneTilingJobMapper.from(detection);
    var job = zoneTilingJobMapper.toDomain(createJob);
    var tilingTasks = getTilingTasks(createJob, job.getId());

    return zoneTilingJobService.create(job, tilingTasks);
  }

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
