package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ParcelService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ZoneTilingJobMapper {
  private final ParcelService parcelService;
  private final StatusMapper<JobStatus> statusMapper;
  private final ZoomMapper zoomMapper;

  public ZoneTilingJob toDomain(CreateZoneTilingJob rest) {
    var generatedId = randomUUID();
    var job =
        ZoneTilingJob.builder()
            .id(generatedId.toString())
            .zoneName(rest.getZoneName())
            .emailReceiver(rest.getEmailReceiver())
            .submissionInstant(now())
            .build();
    job.hasNewStatus(
        JobStatus.builder()
            .health(UNKNOWN)
            .progression(PENDING)
            .creationDatetime(now())
            .jobId(generatedId.toString())
            .build());
    return job;
  }

  public app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(ZoneTilingJob domain) {
    var parcel0 =
        parcelService
            .getParcelsByJobId(domain.getId())
            // TODO: only need one
            .get(0);
    var parcelContent = parcel0.getParcelContent();

    return new app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .creationDatetime(domain.getSubmissionInstant())
        .zoomLevel(
            zoomMapper.toRest(ArcgisImageZoom.fromZoomLevel(parcelContent.getFeature().getZoom())))

        // All parcels of the same job have same geoServer url and parameter
        .geoServerUrl(parcelContent.getGeoServerUrl().toString())
        .geoServerParameter(parcelContent.getGeoServerParameter())
        .emailReceiver(domain.getEmailReceiver())
        .status(statusMapper.statusConverter(domain.getStatus()));
  }
}
