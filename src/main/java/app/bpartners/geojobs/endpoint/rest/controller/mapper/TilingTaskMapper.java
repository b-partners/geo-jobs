package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.Parcel;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.model.Tile;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import java.net.URL;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TilingTaskMapper {
  private final FeatureMapper featureMapper;
  private final ZoneTilingJobRepository zoneTilingJobRepository;

  public TilingTask from(
      Feature createFeature,
      URL geoServerUrl,
      GeoServerParameter geoServerParameter,
      String jobId) {
    String generatedId = randomUUID().toString();
    return TilingTask.builder()
        .id(generatedId)
        .jobId(jobId)
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .health(UNKNOWN)
                    .progression(PENDING)
                    .creationDatetime(now())
                    .taskId(generatedId)
                    .build()))
        .submissionInstant(now())
        .parcel(featureMapper.toDomain(createFeature, geoServerUrl, geoServerParameter))
        .build();
  }

  public Parcel toRest(app.bpartners.geojobs.repository.model.geo.Parcel model, String jobId) {
    ZoneTilingJob zoneTilingJob = zoneTilingJobRepository.findById(jobId).get();
    return new Parcel()
        .id(randomUUID().toString())
        .creationDatetime(model.getCreationDatetime())
        .tiles(model.getTiles().stream().map(this::toRest).toList())
        .tilingStatus(
            ofNullable(zoneTilingJob.getStatus())
                .map(
                    status ->
                        new Status()
                            .health(StatusMapper.toHealthStatus(status.getHealth()))
                            .progression(StatusMapper.toProgressionEnum(status.getProgression()))
                            .creationDatetime(status.getCreationDatetime()))
                .orElse(null))
        .feature(model.getFeature());
  }

  public Tile toRest(app.bpartners.geojobs.repository.model.geo.tiling.Tile model) {
    return new Tile()
        .id(model.getId())
        .coordinates(model.getCoordinates())
        .creationDatetime(model.getCreationDatetime())
        .bucketPath(model.getBucketPath());
  }
}
