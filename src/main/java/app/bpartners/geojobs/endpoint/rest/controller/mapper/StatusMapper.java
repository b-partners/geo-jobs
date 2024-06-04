package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.Status;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StatusMapper<T extends app.bpartners.geojobs.job.model.Status> {

  public static Status.ProgressionEnum toProgressionEnum(
      app.bpartners.geojobs.job.model.Status.ProgressionStatus domain) {
    return switch (domain) {
      case PENDING -> Status.ProgressionEnum.PENDING;
      case PROCESSING -> Status.ProgressionEnum.PROCESSING;
      case FINISHED -> Status.ProgressionEnum.FINISHED;
    };
  }

  public static Status.HealthEnum toHealthStatus(
      app.bpartners.geojobs.job.model.Status.HealthStatus domain) {
    return switch (domain) {
      case SUCCEEDED -> Status.HealthEnum.SUCCEEDED;
      case FAILED -> Status.HealthEnum.FAILED;
      case UNKNOWN -> Status.HealthEnum.UNKNOWN;
      case RETRYING -> Status.HealthEnum.RETRYING;
    };
  }

  public Status toRest(T tilingJobStatus) {
    return new Status()
        .progression(toProgressionEnum(tilingJobStatus.getProgression()))
        .health(toHealthStatus(tilingJobStatus.getHealth()))
        .creationDatetime(tilingJobStatus.getCreationDatetime());
  }
}
