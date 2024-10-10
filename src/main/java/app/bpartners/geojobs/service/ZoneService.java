package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.CONFIGURING;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.TILING;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
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
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final BucketComponent bucketComponent;
  private final GeoJsonConversionInitiationService conversionInitiationService;
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;
  private final ObjectMapper objectMapper;
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
    detection.setGeoJsonZone(readFromFile(featuresFromShape));
    var savedDetection = detectionRepository.save(detection);
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeFromConfiguring(savedDetection, PROCESSING, UNKNOWN);
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
    return computeFromConfiguring(savedDetection, PENDING, UNKNOWN);
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
    return computeFromConfiguring(savedDetection, PENDING, UNKNOWN);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection getProcessedDetection(
      String detectionId) {
    var detection = getDetectionByE2eId(detectionId);
    if (detection.getGeoJsonZone() == null || detection.getGeoJsonZone().isEmpty()) {
      return computeFromConfiguring(detection, PENDING, UNKNOWN);
    }
    if (!ROLE_ADMIN.equals(authProvider.getPrincipal().getRole())) {
      return computeFromConfiguring(detection, FINISHED, SUCCEEDED);
    }
    var detectionJobId = detection.getZdjId();
    if (detectionJobId == null) {
      return getTilingStatistics(detection, detection.getZtjId());
    }
    return getDetectionStatistics(detection, detectionJobId);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection processZoneDetection(
      String detectionId, CreateDetection createDetection, @Nullable String communityOwnerId) {
    Optional<Detection> optionalDetection = detectionRepository.findByEndToEndId(detectionId);
    if (optionalDetection.isEmpty()) {
      var savedDetection = createZoneDetectionJob(detectionId, createDetection, communityOwnerId);
      return computeFromConfiguring(savedDetection, PENDING, UNKNOWN);
    }
    if (ROLE_COMMUNITY.equals(authProvider.getPrincipal().getRole())) {
      throw new BadRequestException(
          String.format(
              "A detectionJob with the specified id=(%s) "
                  + "already exists and can not be updated.",
              detectionId));
    }
    return getProcessingJobStatistics(optionalDetection.get());
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection getProcessingJobStatistics(
      Detection detection) {
    var tilingJobId = detection.getZtjId();
    var detectionJobId = detection.getZdjId();
    if (detection.getGeoJsonZone() == null || detection.getGeoJsonZone().isEmpty()) {
      return computeFromConfiguring(detection, PENDING, UNKNOWN);
    }
    if (tilingJobId == null) {
      var ztj = processZoneTilingJob(detection);
      var detectionWithZTJ =
          detectionRepository.save(detection.toBuilder().ztjId(ztj.getId()).build());
      return getTilingStatistics(detectionWithZTJ, ztj.getId());
    }
    var zoneTilingJob = zoneTilingJobService.findById(tilingJobId);
    if (!zoneTilingJob.isSucceeded()) {
      return getTilingStatistics(detection, tilingJobId);
    }
    var machineZoneDetectionJob = zoneDetectionJobService.findById(detectionJobId);

    if (machineZoneDetectionJob.isPending() && zoneTilingJob.isFinished()) {
      var savedDetectionJob = processZoneDetectionJob(detection, zoneTilingJob);
      return getDetectionStatistics(detection, savedDetectionJob.getId());
    }
    if (machineZoneDetectionJob.isFinished()) {
      var humanZoneDetectionJob = zoneDetectionJobService.getByTilingJobId(tilingJobId, HUMAN);
      processVerificationOrGenerateGeoJson(detection, humanZoneDetectionJob);
    }
    return getDetectionStatistics(detection, machineZoneDetectionJob.getId());
  }

  private void processVerificationOrGenerateGeoJson(
      Detection detection, ZoneDetectionJob humanZoneDetectionJob) {
    if (!humanZoneDetectionJob.isFinished()) {
      eventProducer.accept(
          List.of(
              AnnotationJobVerificationSent.builder()
                  .humanZdjId(humanZoneDetectionJob.getId())
                  .build()));
    } else {
      conversionInitiationService.processConversionTask(
          detection, humanZoneDetectionJob.getZoneName(), humanZoneDetectionJob.getId());
    }
  }

  private Detection createZoneDetectionJob(
      String detectionId, CreateDetection createDetection, @Nullable String communityOwnerId) {
    var detectionToSave =
        mapFromRestCreateDetection(detectionId, createDetection, communityOwnerId);
    var savedDetection =
        communityUsedSurfaceService.persistDetectionWithSurfaceUsage(
            detectionToSave, createDetection.getGeoJsonZone());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return savedDetection;
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
      return toRestDetection(
          detection,
          zoneDetectionJobService.getTaskStatistic(detection.getZdjId()),
          MACHINE_DETECTION);
    }

    if (detection.getZtjId() != null) {
      return toRestDetection(
          detection, zoneTilingJobService.getTaskStatistic(detection.getZtjId()), TILING);
    }

    return toRestDetection(detection, new TaskStatistic(), CONFIGURING);
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
    return toRestDetection(detection, defaultConfiguringStatistic, CONFIGURING);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection getTilingStatistics(
      Detection detection, String tilingJobId) {
    return toRestDetection(
        detection, zoneTilingJobService.computeTaskStatistics(tilingJobId), TILING);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection getDetectionStatistics(
      Detection detection, String detectionJobId) {
    return toRestDetection(
        detection,
        zoneDetectionJobService.computeTaskStatistics(detectionJobId),
        MACHINE_DETECTION);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection toRestDetection(
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
