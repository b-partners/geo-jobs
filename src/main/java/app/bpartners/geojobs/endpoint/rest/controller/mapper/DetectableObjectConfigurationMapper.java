package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class DetectableObjectConfigurationMapper {
    private final DetectableObjectTypeMapper typeMapper;
    public DetectableObjectConfiguration toDomain(String jobId,
                                                  app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration rest) {
        return DetectableObjectConfiguration.builder()
                .id(randomUUID().toString())
                .detectionJobId(jobId)
                .objectType(typeMapper.toDomain(rest.getType()))
                .confidence(Objects.requireNonNull(rest.getConfidence()).doubleValue())
                .build();
    }
}
