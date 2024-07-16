package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ParcelService;
import java.util.List;
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

  public app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(
      ZoneTilingJob domain, List<TilingTask> tilingTaskList) {
    var parcels = parcelService.getParcelsByJobId(domain.getId());
    return toRest(domain, tilingTaskList, parcels);
  }

  public app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(
      ZoneTilingJob domain, List<TilingTask> tilingTaskList, boolean jobNotSaved) {
    List<Parcel> parcels =
        jobNotSaved ? List.of() : parcelService.getParcelsByJobId(domain.getId());
    return toRest(domain, tilingTaskList, parcels);
  }

  private app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(
      ZoneTilingJob domain, List<TilingTask> tilingTaskList, List<Parcel> parcels) {
    var parcel0 = parcels.isEmpty() ? null : parcels.get(0); // only need one
    var parcelContent = parcel0 == null ? null : parcel0.getParcelContent();

    return new app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .creationDatetime(domain.getSubmissionInstant())
        .zoomLevel(
            parcel0 == null
                ? null
                : (parcelContent.getFeature() == null
                    ? null
                    : zoomMapper.toRest(
                        ArcgisImageZoom.fromZoomLevel(parcelContent.getFeature().getZoom()))))

        // All parcels of the same job have same geoServer url and parameter
        .geoServerUrl(parcel0 == null ? null : parcelContent.getGeoServerUrl().toString())
        .geoServerParameter(parcel0 == null ? null : parcelContent.getGeoServerParameter())
        .emailReceiver(domain.getEmailReceiver())
        .features(tilingTaskList.stream().map(FeatureMapper::from).toList())
        .status(statusMapper.toRest(domain.getStatus()));
  }

  public CreateZoneTilingJob from(CreateFullDetection zoneToDetect) {
    CreateFullDetection.ZoomLevelEnum zoom = zoneToDetect.getZoomLevel();
    assert zoom != null;
    CreateZoneTilingJob.ZoomLevelEnum zoomLevelEnum =
        CreateZoneTilingJob.ZoomLevelEnum.valueOf(zoom.getValue());
    return new CreateZoneTilingJob()
        .emailReceiver(zoneToDetect.getEmailReceiver())
        .zoneName(zoneToDetect.getZoneName())
        .geoServerParameter(zoneToDetect.getGeoServerParameter())
        .geoServerUrl(zoneToDetect.getGeoServerUrl())
        .features(zoneToDetect.getFeatures())
        .zoomLevel(zoomLevelEnum);
  }
}
