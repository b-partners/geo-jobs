package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.geo.ParcelService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ZoneTilingJobMapper {
  private final TilingTaskMapper taskMapper;
  private final ParcelService parcelService;
  private final StatusMapper<JobStatus> statusMapper;

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

    return new app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .creationDatetime(domain.getSubmissionInstant())
        // .zoomLevel() //TODO

        // All parcels of the same job have same geoServer url and parameter
        .geoServerUrl(parcel0.getGeoServerUrl().toString())
        .geoServerParameter(parcel0.getGeoServerParameter())
        .emailReceiver(domain.getEmailReceiver())
        .status(statusMapper.statusConverter(domain.getStatus()));
  }
}
