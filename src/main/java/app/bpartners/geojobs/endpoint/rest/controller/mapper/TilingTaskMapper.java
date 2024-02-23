package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.Parcel;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.endpoint.rest.model.Tile;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.net.URL;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TilingTaskMapper {
  private final FeatureMapper featureMapper;

  public TilingTask from(
      Feature createFeature,
      URL geoServerUrl,
      GeoServerParameter geoServerParameter,
      String jobId) {
    String generatedTaskId = randomUUID().toString();
    String generatedParcelId = randomUUID().toString();
    return TilingTask.builder()
        .id(generatedTaskId)
        .jobId(jobId)
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .health(UNKNOWN)
                    .progression(PENDING)
                    .creationDatetime(now())
                    .taskId(generatedTaskId)
                    .build()))
        .submissionInstant(now())
        .parcels(
            List.of(
                featureMapper.toDomain(
                    generatedParcelId,
                    createFeature,
                    geoServerUrl,
                    geoServerParameter))) // TODO: check when multiple parcels for tiling task
        .build();
  }

  public Parcel toRest(app.bpartners.geojobs.repository.model.Parcel model) {
    var parcelContent = model.getParcelContent();
    return new Parcel()
        .id(model.getId())
        .creationDatetime(parcelContent.getCreationDatetime())
        .tiles(parcelContent.getTiles().stream().map(this::toRest).toList())
        .tilingStatus(
            ofNullable(parcelContent.getTilingStatus())
                .map(
                    status ->
                        new Status()
                            .health(StatusMapper.toHealthStatus(status.getHealth()))
                            .progression(StatusMapper.toProgressionEnum(status.getProgression()))
                            .creationDatetime(status.getCreationDatetime()))
                .orElse(null))
        .feature(parcelContent.getFeature());
  }

  public Tile toRest(app.bpartners.geojobs.repository.model.tiling.Tile model) {
    return new Tile()
        .id(model.getId())
        .coordinates(model.getCoordinates())
        .creationDatetime(model.getCreationDatetime())
        .bucketPath(model.getBucketPath());
  }
}
