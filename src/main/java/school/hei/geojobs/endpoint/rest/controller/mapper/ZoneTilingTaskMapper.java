package school.hei.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.geojobs.repository.ZoneTilingJobRepository;
import school.hei.geojobs.repository.model.TilingTaskStatus;
import school.hei.geojobs.repository.model.ZoneTilingJob;
import school.hei.geojobs.repository.model.ZoneTilingTask;
import school.hei.geojobs.endpoint.rest.model.Feature;
import school.hei.geojobs.endpoint.rest.model.GeoServerParameter;
import school.hei.geojobs.endpoint.rest.model.Parcel;
import school.hei.geojobs.endpoint.rest.model.Status;
import school.hei.geojobs.endpoint.rest.model.Tile;

@Component
@AllArgsConstructor
public class ZoneTilingTaskMapper {
  private final FeatureMapper featureMapper;
  private final ZoneTilingJobRepository zoneTilingJobRepository;

  public ZoneTilingTask from(
      Feature createFeature, URL geoServerUrl, GeoServerParameter geoServerParameter, UUID jobId) {
    String generatedId = randomUUID().toString();
    return ZoneTilingTask.builder()
        .id(generatedId)
        .jobId(jobId.toString())
        .statusHistory(
            List.of(
                TilingTaskStatus.builder()
                    .health(school.hei.geojobs.repository.model.Status.HealthStatus.UNKNOWN)
                    .progression(school.hei.geojobs.repository.model.Status.ProgressionStatus.PENDING)
                    .creationDatetime(now())
                    .taskId(generatedId)
                    .build()))
        .submissionInstant(now())
        .parcel(featureMapper.toDomain(createFeature, geoServerUrl, geoServerParameter))
        .build();
  }

  public Parcel toRest(school.hei.geojobs.repository.model.geo.Parcel model, String jobId) {
    ZoneTilingJob zoneTilingJob = zoneTilingJobRepository.findById(jobId).get();
    return new Parcel()
        .id(UUID.randomUUID().toString())
        .creationDatetime(Instant.parse(model.getCreationDatetime()))
        .tiles(
            ofNullable(model.getTiles())
                .map(tiles -> tiles.stream().map(this::toRest).toList())
                .orElse(null))
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

  public Tile toRest(school.hei.geojobs.repository.model.Tile model) {
    return new Tile()
        .id(model.getId())
        .coordinates(model.getCoordinates())
        .creationDatetime(Instant.parse(model.getCreationDatetime()))
        .bucketPath(model.getBucketPath());
  }
}
