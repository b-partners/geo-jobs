package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.*;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureMultiPolygonChecker;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneService {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneTilingJobService zoneTilingJobService;
  private final EventProducer eventProducer;
  private final DetectionRepository detectionRepository;
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final BucketComponent bucketComponent;
  private final GeoJsonConversionJobService conversionInitiationService;
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;
  private final ObjectMapper objectMapper;
  private final AuthProvider authProvider;
  private final DetectionGeoJsonUpdateValidator detectionGeoJsonUpdateValidator;
  private final FeatureMultiPolygonChecker featureMultiPolygonChecker;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final DetectionTilingCreation detectionTilingCreation;
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;
  private final DetectionTilingStatisticsComputer detectionTilingStatisticsComputer;
  private final DetectionMachineDetectionStatisticsComputer
      detectionMachineDetectionStatisticsComputer;
  private final DetectionMachineDetectionCreation detectionMachineDetectionCreation;
  private final GeoJsonConversionJobRepository geoJsonConversionJobRepository;

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
    if (detection.getProvidedGeoJsonZone() != null
        && !detection.getProvidedGeoJsonZone().isEmpty()) {
      throw new BadRequestException(
          "Unable to finalize Detection(id=" + detectionId + ") geoJson as it already has values");
    }
    var features =
        readFromFile(featuresFromShape).stream().map(FeatureMapper::toDomainFeature).toList();
    detection.setProvidedGeoJsonZone(features);
    var savedDetection = detectionRepository.save(detection);
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeEmptyStatisticFromStep(savedDetection, PROCESSING, UNKNOWN, CONFIGURING);
  }

  private Detection getDetectionById(String detectionId) {
    return detectionRepository
        .findById(detectionId)
        .orElseThrow(() -> new NotFoundException("Detection(id=" + detectionId + ") not found"));
  }

  private Detection getDetectionByE2eId(String detectionId, String communityOwnerId) {
    return detectionRepository
        .findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Detection(e2e.id="
                        + detectionId
                        + ") not found for communityOwnerId="
                        + communityOwnerId));
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection configureExcelFile(
      String detectionId, File excelFile) {
    var detection = getDetectionByE2IdOrId(detectionId);
    detectionGeoJsonUpdateValidator.accept(detection);
    var bucketKey = "detections/excel/" + detectionId;
    bucketComponent.upload(excelFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().excelFileKey(bucketKey).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeEmptyStatisticFromStep(savedDetection, PENDING, UNKNOWN, CONFIGURING);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection configureShapeFile(
      String detectionId, File shapeFile) {
    var detection = getDetectionByE2IdOrId(detectionId);
    detectionGeoJsonUpdateValidator.accept(detection);
    var bucketKey = "detections/shape/" + detectionId;
    bucketComponent.upload(shapeFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().shapeFileKey(bucketKey).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeEmptyStatisticFromStep(savedDetection, PENDING, UNKNOWN, CONFIGURING);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection configureGeoJsonResult(
      String detectionId, File geoJsonFile) {
    var detection = getDetectionById(detectionId);
    var geoJsonResultFileKey =
        GEO_JSON_BUCKET_FOLDER
            + detection.getId()
            + "/"
            + detection.getZoneName()
            + GEO_JSON_EXTENSION;
    bucketComponent.upload(geoJsonFile, geoJsonResultFileKey);
    var savedDetection =
        detectionRepository.save(
            detection.toBuilder().geojsonS3FileKey(geoJsonResultFileKey).build());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return computeEmptyStatisticFromStep(detection, FINISHED, SUCCEEDED, GEO_JSON_CONVERSION);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection getProcessedDetection(
      String detectionId) {
    var detection = getDetectionByE2IdOrId(detectionId);
    if (detection.isSucceeded()) {
      return computeEmptyStatisticFromStep(detection, FINISHED, SUCCEEDED, GEO_JSON_CONVERSION);
    }
    if (detection.isStillOnConfiguringStep()) {
      return computeEmptyStatisticFromStep(detection, PENDING, UNKNOWN, CONFIGURING);
    }
    if (!communityHasAdminRole()) {
      return computeEmptyStatisticFromStep(detection, FINISHED, SUCCEEDED, CONFIGURING);
    }
    if (detection.isStillOnTilingStep()) {
      if (detection.isTilingPending()) {
        return computeEmptyStatisticFromStep(detection, PENDING, UNKNOWN, TILING);
      }
      return detectionTilingStatisticsComputer.apply(detection, detection.getZtjId());
    }
    var zoneDetectionJob = zoneDetectionJobService.findById(detection.getZdjId());
    if (detection.isMachineDetectionStepProcessing(zoneDetectionJob)) {
      return detectionMachineDetectionStatisticsComputer.apply(detection, detection.getZdjId());
    }
    if (detection.isHumanDetectionStepProcessing(zoneDetectionJob)) {
      var inDoubtDetectedTileToDelivery =
          zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(zoneDetectionJob.getId());
      if (inDoubtDetectedTileToDelivery > 0) {
        return computeEmptyStatisticFromStep(detection, PROCESSING, UNKNOWN, HUMAN_DETECTION);
      }
      var geoJsonConversionJob = findActualGeoJsonConversionJob(zoneDetectionJob.getId());
      if (geoJsonConversionJob != null) {
        if (geoJsonConversionJob.isSucceeded()) {
          return computeEmptyStatisticFromStep(detection, FINISHED, SUCCEEDED, GEO_JSON_CONVERSION);
        }
        if (geoJsonConversionJob.isProcessing()) {
          return computeEmptyStatisticFromStep(detection, PROCESSING, UNKNOWN, GEO_JSON_CONVERSION);
        }
        if (geoJsonConversionJob.isPending()) {
          return computeEmptyStatisticFromStep(detection, PENDING, UNKNOWN, GEO_JSON_CONVERSION);
        }
      }
      return detectionMachineDetectionStatisticsComputer.apply(detection, detection.getZdjId());
    }
    throw new IllegalStateException(
        "Detection(id=" + detection.getId() + ") processing failed on illegal state");
  }

  private Detection getDetectionByE2IdOrId(String detectionId) {
    var communityAuthorization =
        communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
    return communityOwnerId != null
        ? getDetectionByE2eId(detectionId, communityOwnerId)
        : getDetectionById(detectionId);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection processDetection(
      String detectionId,
      CreateDetection createDetection,
      String communityOwnerId,
      boolean isRooferMade) {
    var optionalDetection =
        detectionRepository.findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId);
    if (optionalDetection.isEmpty()) {
      var savedDetection =
          createDetectionJob(detectionId, createDetection, communityOwnerId, isRooferMade);
      if (savedDetection.isStillOnConfiguringStep()) {
        return computeEmptyStatisticFromStep(savedDetection, PENDING, UNKNOWN, CONFIGURING);
      }
      return detectionTilingCreation.apply(savedDetection);
    }
    if (isRooferMade) {
      return processRooferDetection(detectionId);
    }
    return processCommunityDetection(detectionId);
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection processRooferDetection(
      String detectionId) {
    return new app.bpartners.geojobs.endpoint.rest.model
        .Detection(); // TODO: detection logic after tiling
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection processCommunityDetection(
      String detectionId) {
    var detection =
        detectionRepository
            .findById(detectionId)
            .or(() -> detectionRepository.findByEndToEndId(detectionId))
            .orElseThrow(
                () -> new NotFoundException("Detection(id=" + detectionId + ") not found"));
    return getProcessingJobStatistics(detection);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection getProcessingJobStatistics(
      Detection detection) {
    var tilingJobId = detection.getZtjId();
    var detectionJobId = detection.getZdjId();
    if (detection.isStillOnConfiguringStep()) {
      return computeEmptyStatisticFromStep(detection, PENDING, UNKNOWN, CONFIGURING);
    }
    if (tilingJobId == null) {
      return detectionTilingCreation.apply(detection);
    }
    var zoneTilingJob = zoneTilingJobService.findById(tilingJobId);
    if (!zoneTilingJob.isSucceeded()) {
      return detectionTilingStatisticsComputer.apply(detection, tilingJobId);
    }
    var machineZoneDetectionJob = zoneDetectionJobService.findById(detectionJobId);

    if (machineZoneDetectionJob.isPending() && zoneTilingJob.isFinished()) {
      detectionMachineDetectionCreation.apply(detection, zoneTilingJob);
    }
    if (machineZoneDetectionJob.isFinished()) {
      if (zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(detectionJobId) == 0L) {
        processVerificationOrGenerateGeoJson(detection, machineZoneDetectionJob);
      } else {
        var humanZoneDetectionJob = zoneDetectionJobService.getByTilingJobId(tilingJobId, HUMAN);
        processVerificationOrGenerateGeoJson(detection, humanZoneDetectionJob);
      }
    }
    return detectionMachineDetectionStatisticsComputer.apply(
        detection, machineZoneDetectionJob.getId());
  }

  private void processVerificationOrGenerateGeoJson(
      Detection detection, ZoneDetectionJob zoneDetectionJob) {
    if (HUMAN.equals(zoneDetectionJob.getDetectionType()) && !zoneDetectionJob.isSucceeded()) {
      eventProducer.accept(
          List.of(
              AnnotationJobVerificationSent.builder()
                  .humanZdjId(zoneDetectionJob.getId())
                  .build()));
    } else {
      conversionInitiationService.getOrComputeGeoJsonConversionJob(detection, zoneDetectionJob);
    }
  }

  private Detection createDetectionJob(
      String detectionId,
      CreateDetection createDetection,
      @Nullable String communityOwnerId,
      boolean isRooferMade) {
    var detectionToSave =
        mapFromRestCreateDetection(detectionId, createDetection, communityOwnerId, isRooferMade);
    var savedDetection =
        communityUsedSurfaceService.persistDetectionWithSurfaceUsage(
            detectionToSave, createDetection.getGeoJsonZone());
    eventProducer.accept(List.of(DetectionSaved.builder().detection(savedDetection).build()));
    return savedDetection;
  }

  private Detection mapFromRestCreateDetection(
      String endToEndId,
      CreateDetection createDetection,
      @Nullable String communityOwnerId,
      boolean isRooferMade) {
    var detectableObjectModel = createDetection.getDetectableObjectModel();
    var modelActualInstance = Objects.requireNonNull(detectableObjectModel).getActualInstance();
    var detectionId = randomUUID().toString();
    var detectableObjectConfigurations =
        detectableObjectTypeMapper.mapDefaultConfigurationsFromModel(
            detectionId, modelActualInstance);
    var providedGeoJsonZone =
        createDetection.getGeoJsonZone().stream().map(FeatureMapper::toDomainFeature).toList();
    var featuresHasAllMultiPolygonInstances =
        featureMultiPolygonChecker.apply(createDetection.getGeoJsonZone());
    var detectionBuilder =
        Detection.builder()
            .id(detectionId)
            .endToEndId(endToEndId)
            .emailReceiver(createDetection.getEmailReceiver())
            .zoneName(createDetection.getZoneName())
            .isRooferMade(isRooferMade)
            .communityOwnerId(communityOwnerId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .geoServerProperties(createDetection.getGeoServerProperties())
            .providedGeoJsonZone(providedGeoJsonZone);
    if (featuresHasAllMultiPolygonInstances) {
      detectionBuilder.multiPolygonGeoJsonZone(providedGeoJsonZone);
    }
    if (modelActualInstance instanceof BPToitureModel) {
      detectionBuilder.bpToitureModel((BPToitureModel) modelActualInstance);
    } else if (modelActualInstance instanceof BPLomModel) {
      detectionBuilder.bpLomModel((BPLomModel) modelActualInstance);
    } else if (modelActualInstance instanceof BPZanModel) {
      detectionBuilder.bpZanModel((BPZanModel) modelActualInstance);
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
      return detectionFromStatisticRestMapper.apply(
          detection,
          zoneDetectionJobService.getTaskStatistic(detection.getZdjId()),
          MACHINE_DETECTION);
    }

    if (detection.getZtjId() != null) {
      return detectionFromStatisticRestMapper.apply(
          detection, zoneTilingJobService.getTaskStatistic(detection.getZtjId()), TILING);
    }
    return detectionFromStatisticRestMapper.apply(detection, new TaskStatistic(), CONFIGURING);
  }

  private app.bpartners.geojobs.endpoint.rest.model.Detection computeEmptyStatisticFromStep(
      Detection detection,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus,
      DetectionStepName detectionStepName) {
    var geoJobType = fromDetectionStep(detectionStepName);
    var emptyStatistic =
        TaskStatistic.builder()
            .jobType(geoJobType)
            .actualJobStatus(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .creationDatetime(now())
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .jobType(geoJobType)
                    .build())
            .updatedAt(now())
            .taskStatusStatistics(List.of())
            .build();
    return detectionFromStatisticRestMapper.apply(detection, emptyStatistic, detectionStepName);
  }

  private GeoJobType fromDetectionStep(DetectionStepName stepName) {
    return switch (stepName) {
      case TILING -> GeoJobType.TILING;
      case CONFIGURING -> GeoJobType.CONFIGURING;
      case MACHINE_DETECTION, HUMAN_DETECTION -> GeoJobType.DETECTION;
      case GEO_JSON_CONVERSION -> GeoJobType.GEO_JSON_CONVERSION;
    };
  }

  private GeoJsonConversionJob findActualGeoJsonConversionJob(String zoneDetectionJobId) {
    var geoJsonConversionJobs =
        geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJobId);
    return geoJsonConversionJobs.stream()
        .filter(Job::isSucceeded)
        .findFirst()
        .orElseGet(
            () ->
                geoJsonConversionJobs.stream()
                    .max(Comparator.comparing(Job::getSubmissionInstant))
                    .orElse(null));
  }

  private boolean communityHasAdminRole() {
    return authProvider.getPrincipal().isAdmin();
  }
}
