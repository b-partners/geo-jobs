package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.time.LocalDate.now;
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
  public static String BUCKET_STORAGE_NAME = "TILING_IMAGES";

  public DetectableObjectConfiguration toDomain(
      String jobId, app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration rest) {
    return DetectableObjectConfiguration.builder()
        .id(randomUUID().toString())
        .detectionJobId(jobId)
        .objectType(typeMapper.toDomain(Objects.requireNonNull(rest.getType())))
        .confidence(rest.getConfidence() != null ? rest.getConfidence().doubleValue() : 1)
        .bucketStorageName(
            rest.getBucketStorageName() != null
                ? rest.getBucketStorageName()
                : BUCKET_STORAGE_NAME + "_" + now())
        .build();
  }

  public app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration toRest(
      DetectableObjectConfiguration domain) {
    return new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
        .confidence(
            domain.getConfidence() == null
                ? BigDecimal.valueOf(1)
                : BigDecimal.valueOf(domain.getConfidence()))
        .type(typeMapper.toRest(domain.getObjectType()))
        .bucketStorageName(domain.getBucketStorageName());
  }
}
