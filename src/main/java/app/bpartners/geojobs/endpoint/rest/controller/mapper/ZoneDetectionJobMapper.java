package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ZoneDetectionJobMapper {
  private final StatusMapper<JobStatus> statusMapper;

  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob toRest(
      ZoneDetectionJob domain,
      List<app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration>
          detectableObjectConfigurations) {
    return new app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .emailReceiver(domain.getEmailReceiver())
        .zoneTilingJobId(domain.getZoneTilingJob().getId())
        .objectsToDetect(detectableObjectConfigurations)
        .creationDatetime(domain.getSubmissionInstant())
        .status(statusMapper.statusConverter(domain.getStatus()));
  }
}
