package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
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

  @PostMapping("/detectionJobs/{id}/process")
  public List<app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob> processZDJ(
      @PathVariable("id") String jobId,
      @RequestBody List<DetectableObjectConfiguration> detectableObjectConfigurations) {

    List<ZoneDetectionJob> processedZDJ = service.fireTasks(jobId);

    return processedZDJ.stream()
        .map(job -> mapper.toRest(job, detectableObjectConfigurations))
        .toList();
  }
}
