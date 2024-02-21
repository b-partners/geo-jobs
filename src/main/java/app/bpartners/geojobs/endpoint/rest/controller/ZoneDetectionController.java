package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Slf4j
public class ZoneDetectionController {
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final ZoneDetectionJobService service;
  private final ZoneDetectionJobMapper mapper;
  private final DetectableObjectConfigurationMapper objectConfigurationMapper;

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
    List<app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration>
        configurations =
            detectableObjectConfigurations.stream()
                .map(objectConf -> objectConfigurationMapper.toDomain(jobId, objectConf))
                .toList();
    ZoneDetectionJob processedZDJ = service.fireTasks(jobId, configurations);
    return mapper.toRest(processedZDJ, detectableObjectConfigurations);
  }
}
