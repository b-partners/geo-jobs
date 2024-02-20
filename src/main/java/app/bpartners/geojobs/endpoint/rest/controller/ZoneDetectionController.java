package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class ZoneDetectionController {
  private final ZoneDetectionJobService service;
  private final ZoneDetectionJobMapper mapper;
  private final DetectableObjectConfigurationMapper objectConfigurationMapper;

  @PostMapping("/detectionJobs/{id}/process")
  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob processZDJ(
      @PathVariable("id") String jobId,
      @RequestBody List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    List<app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration> configurations = detectableObjectConfigurations.stream().map(objectConf -> objectConfigurationMapper.toDomain(jobId, objectConf)).toList();
    ZoneDetectionJob processedZDJ = service.fireTasks(jobId, configurations);
    return mapper.toRest(processedZDJ, detectableObjectConfigurations);
  }
}
