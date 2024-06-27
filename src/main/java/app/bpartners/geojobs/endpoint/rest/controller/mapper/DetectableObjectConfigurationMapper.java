package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DetectableObjectConfigurationMapper {
  private final DetectableObjectTypeMapper typeMapper;

  public DetectableObjectConfiguration toDomain(
      String jobId, app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration rest) {
    return DetectableObjectConfiguration.builder()
        .id(randomUUID().toString())
        .detectionJobId(jobId)
        .objectType(typeMapper.toDomain(Objects.requireNonNull(rest.getType())))
        .confidence(Objects.requireNonNull(rest.getConfidence()).doubleValue())
        .bucketStorageName(rest.getBucketStorageName())
        .build();
  }

  public app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration toRest(
      DetectableObjectConfiguration domain) {
    return new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
        .confidence(
            domain.getConfidence() == null
                ? null
                : BigDecimal.valueOf(domain.getConfidence())) // TODO: Unknown default value
        .type(typeMapper.toRest(domain.getObjectType()))
        .bucketStorageName(domain.getBucketStorageName());
  }
}
