package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.SuccessStatus.NOT_SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.SuccessStatus.SUCCEEDED;
import static app.bpartners.geojobs.file.ExtensionGuesser.OFFICE_OPEN_XML_FILE_MEDIA_TYPE;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionSurfaceUnitMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionTaskMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectedParcel;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.endpoint.rest.model.DetectionSurfaceUnit;
import app.bpartners.geojobs.endpoint.rest.model.DetectionUsage;
import app.bpartners.geojobs.endpoint.rest.model.FilteredDetectionJob;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonsUrl;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.model.TaskStatistic;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.DetectionAuthorizer;
import app.bpartners.geojobs.endpoint.rest.validator.GetUsageValidator;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.MediaTypeGuesser;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import app.bpartners.geojobs.service.ParcelService;
import app.bpartners.geojobs.service.ZoneService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import java.io.File;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class ZoneDetectionController {
  private final ParcelService parcelService;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final ZoneDetectionJobService service;
  private final ZoneDetectionJobMapper mapper;
  private final DetectableObjectConfigurationMapper objectConfigurationMapper;
  private final DetectionTaskMapper taskMapper;
  private final ZoneDetectionJobValidator jobValidator;
  private final TaskStatisticMapper taskStatisticMapper;
  private final StatusMapper<JobStatus> jobStatusMapper;
  private final EventProducer eventProducer;
  private final GeoJsonConversionInitiationService geoJsonConversionInitiationService;
  private final ZoneService zoneService;
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final GetUsageValidator getUsageValidator;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final AuthProvider authProvider;
  private final DetectionSurfaceUnitMapper unitMapper;
  private final DetectionAuthorizer detectionAuthorizer;
  private final FileWriter fileWriter;
  private final MediaTypeGuesser mediaTypeGuesser;

  @PutMapping("/detectionJobs/{id}/taskFiltering")
  public List<FilteredDetectionJob> filteredDetectionJobs(@PathVariable String id) {
    var filteredTilingJob = service.dispatchTasksBySucceededStatus(id);
    return List.of(
        new FilteredDetectionJob()
            .status(SUCCEEDED)
            .job(mapper.toRest(filteredTilingJob.getSucceededJob(), List.of())),
        new FilteredDetectionJob()
            .status(NOT_SUCCEEDED)
            .job(mapper.toRest(filteredTilingJob.getNotSucceededJob(), List.of())));
  }

  @GetMapping("/detectionJobs/{id}/recomputedParcelsStatuses")
  public Status getZDJTasksRecomputedStatus(@PathVariable String id) {
    var detectionJob = service.findById(id);
    JobStatus jobStatus = detectionJob.getStatus();
    if (!jobStatus.getProgression().equals(FINISHED)) {
      eventProducer.accept(List.of(new ZDJParcelsStatusRecomputingSubmitted(id)));
    }
    return jobStatusMapper.toRest(jobStatus);
  }

  @GetMapping("/detectionJobs/{id}/recomputedStatus")
  public Status getZDJRecomputedStatus(@PathVariable String id) {
    var detectionJob = service.findById(id);
    JobStatus jobStatus = detectionJob.getStatus();
    if (!jobStatus.getProgression().equals(FINISHED)) {
      eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(id)));
    }
    return jobStatusMapper.toRest(jobStatus);
  }

  @GetMapping("/detectionJobs/{id}/taskStatistics")
  public TaskStatistic getDetectionTaskStatistics(@PathVariable String id) {
    return taskStatisticMapper.toRest(service.computeTaskStatistics(id));
  }

  @PutMapping("/detectionJobs/{id}/retry")
  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob processFailedDetectionJob(
      @PathVariable String id) {
    return mapper.toRest(
        service.retryFailedTask(id), List.of()); // TODO: check if features must be returned
  }

  @PostMapping("/detectionJobs/{annotationJobId}/humanVerificationStatus")
  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob checkHumanDetectionJobStatus(
      @PathVariable String annotationJobId) {
    var job = service.checkHumanDetectionJobStatus(annotationJobId);
    var objectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(job.getId()).stream()
            .map(objectConfigurationMapper::toRest)
            .toList();
    return mapper.toRest(job, objectConfigurations);
  }

  @GetMapping("/detectionJobs/{id}/detectedParcels")
  public List<DetectedParcel> getZDJParcels(@PathVariable(name = "id") String detectionJobId) {
    return parcelService.getParcelsByJobId(detectionJobId).stream()
        .map(parcel -> taskMapper.toRest(detectionJobId, parcel))
        .toList();
  }

  @GetMapping("/detectionJobs")
  public List<app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob> getDetectionJobs(
      @RequestParam PageFromOne page, @RequestParam BoundedPageSize pageSize) {
    return service.findAll(page, pageSize).stream()
        .map(
            zdj -> {
              var jobId = zdj.getId();
              var objectConfigurations =
                  objectConfigurationRepository.findAllByDetectionJobId(jobId).stream()
                      .map(objectConfigurationMapper::toRest)
                      .toList();
              return mapper.toRest(zdj, objectConfigurations);
            })
        .toList();
  }

  @PostMapping("/detectionJobs/{id}/process")
  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob processZDJ(
      @PathVariable("id") String jobId,
      @RequestBody List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    jobValidator.accept(jobId);
    List<app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration>
        configurations =
            detectableObjectConfigurations.stream()
                .map(objectConf -> objectConfigurationMapper.toDomain(jobId, objectConf))
                .toList();
    ZoneDetectionJob processedZDJ = service.fireTasks(jobId, configurations);
    return mapper.toRest(processedZDJ, detectableObjectConfigurations);
  }

  @GetMapping("/detectionJobs/{id}/geojsonsUrl")
  public GeoJsonsUrl getZDJGeojsonsUrl(@PathVariable(value = "id") String detectionJobId) {
    return geoJsonConversionInitiationService.initiateGeoJsonConversion(detectionJobId);
  }

  @PostMapping("/detections/{id}/geojson")
  public Detection finalizeShapeConfig(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] featuresFromShape) {
    File featuresFile = fileWriter.apply(featuresFromShape, null);
    return zoneService.finalizeGeoJsonConfig(detectionId, featuresFile);
  }

  @PostMapping("/detections/{id}/shape")
  public Detection configureDetectionShapeFile(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] shapeFileAsBytes) {
    File shapeFile = fileWriter.apply(shapeFileAsBytes, null);
    return zoneService.configureShapeFile(detectionId, shapeFile);
  }

  @PostMapping("/detections/{id}/excel")
  public Detection configureDetectionExcelFile(
      @PathVariable(name = "id") String detectionId, @RequestBody byte[] excelFileAsBytes) {
    var excelFile = fileWriter.apply(excelFileAsBytes, null);
    var guessedMediaType = mediaTypeGuesser.apply(excelFileAsBytes);
    if (!OFFICE_OPEN_XML_FILE_MEDIA_TYPE.equals(guessedMediaType)) {
      throw new BadRequestException(
          "Only open office file (docx, xlsx, xls) media type is accepted but provided was : "
              + guessedMediaType);
    }
    return zoneService.configureExcelFile(detectionId, excelFile);
  }

  @PostMapping("/detections/{id}")
  public Detection processDetection(
      @PathVariable(name = "id") String detectionId, @RequestBody CreateDetection createDetection) {
    detectionAuthorizer.accept(detectionId, createDetection, authProvider.getPrincipal());
    var communityAuthorization =
        communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
    return zoneService.processZoneDetection(detectionId, createDetection, communityOwnerId);
  }

  @GetMapping("/detections/{id}")
  public Detection getProcessedDetection(@PathVariable(name = "id") String detectionId) {
    detectionAuthorizer.accept(detectionId, authProvider.getPrincipal());
    return zoneService.getProcessedDetection(detectionId);
  }

  @GetMapping("/usage")
  public DetectionUsage getDetectionUsage(
      @RequestParam(name = "surfaceUnit", required = false, defaultValue = "SQUARE_DEGREE")
          DetectionSurfaceUnit surfaceUnit) {
    getUsageValidator.accept(authProvider.getPrincipal());
    return communityUsedSurfaceService.getUsage(
        authProvider.getPrincipal(), unitMapper.toDomain(surfaceUnit));
  }

  @GetMapping("/detections")
  public List<Detection> getDetections(
      @RequestParam(name = "page", defaultValue = "1", required = false) PageFromOne page,
      @RequestParam(name = "pageSize", defaultValue = "10", required = false)
          BoundedPageSize pageSize) {
    var communityAuthorization =
        communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId);
    return zoneService.getDetectionsByCriteria(communityOwnerId, page, pageSize);
  }
}
