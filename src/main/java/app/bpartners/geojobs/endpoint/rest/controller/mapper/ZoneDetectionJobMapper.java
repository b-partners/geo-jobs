package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.JobStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ZoneDetectionJobMapper {
  private final StatusMapper<JobStatus> statusMapper;

  public ZoneDetectionJob toRest(
      app.bpartners.geojobs.repository.model.ZoneDetectionJob domain,
      List<app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType> objectTypesToDetect) {
    return new ZoneDetectionJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .emailReceiver(domain.getEmailReceiver())
        .zoneTilingJobId(domain.getZoneTilingJob().getId())
        .objectTypesToDetect(objectTypesToDetect)
        .creationDatetime(domain.getSubmissionInstant())
        .status(statusMapper.statusConverter(domain.getStatus()));
  }
}
