package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.geo.ArcgisImageZoom;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ZoneTilingJobMapper {
  private final TilingTaskMapper taskMapper;
  private final FeatureMapper featureMapper;
  private final StatusMapper<JobStatus> statusMapper;

  public ZoneTilingJob toDomain(CreateZoneTilingJob rest) {
    var generatedId = randomUUID();
    return ZoneTilingJob.builder()
        .id(generatedId.toString())
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .health(UNKNOWN)
                    .progression(PENDING)
                    .creationDatetime(now())
                    .jobId(generatedId.toString())
                    .build()))
        .zoneName(rest.getZoneName())
        .emailReceiver(rest.getEmailReceiver())
        .tasks(
            rest.getFeatures().stream()
                .map(feature -> toTilingTask(feature, generatedId, rest))
                .toList())
        .submissionInstant(now())
        .build();
  }

  private TilingTask toTilingTask(Feature feature, UUID id, CreateZoneTilingJob rest) {
    try {
      feature.setZoom(ArcgisImageZoom.valueOf(rest.getZoomLevel().name()).getZoomLevel());
      return taskMapper.from(
          feature, new URL(rest.getGeoServerUrl()), rest.getGeoServerParameter(), id);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(ZoneTilingJob domain) {
    var parcel0 = domain.getTasks().get(0).getParcel();
    return new app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .creationDatetime(domain.getSubmissionInstant())
        // .zoomLevel() //TODO

        // All parcels of the same job have same geoServer url and parameter
        .geoServerUrl(parcel0.getGeoServerUrl().toString())
        .geoServerParameter(parcel0.getGeoServerParameter())
        .emailReceiver(domain.getEmailReceiver())
        .status(statusMapper.statusConverter(domain.getStatus()))
        .features(domain.getTasks().stream().map(featureMapper::from).toList());
  }
}
