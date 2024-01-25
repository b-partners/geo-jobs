package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.Status;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ZoneTilingJobMapper {
  private final ZoneTilingTaskMapper taskMapper;
  private final FeatureMapper featureMapper;
  private final StatusMapper<JobStatus> statusMapper;

  public app.bpartners.geojobs.repository.model.ZoneTilingJob toDomain(CreateZoneTilingJob rest) {
    var generatedId = randomUUID();
    return app.bpartners.geojobs.repository.model.ZoneTilingJob.builder()
        .id(generatedId.toString())
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .health(Status.HealthStatus.UNKNOWN)
                    .progression(Status.ProgressionStatus.PENDING)
                    .creationDatetime(now())
                    .jobId(generatedId.toString())
                    .build()))
        .zoneName(rest.getZoneName())
        .emailReceiver(rest.getEmailReceiver())
        .tasks(
            rest.getFeatures().stream()
                .map(
                    feature -> {
                      try {
                        return taskMapper.from(
                            feature,
                            new URL(rest.getGeoServerUrl()),
                            rest.getGeoServerParameter(),
                            generatedId);
                      } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                      }
                    })
                .toList())
        .submissionInstant(now())
        .build();
  }

  public ZoneTilingJob toRest(app.bpartners.geojobs.repository.model.ZoneTilingJob domain) {
    var parcel0 = domain.getTasks().get(0).getParcel();
    return new ZoneTilingJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .creationDatetime(domain.getSubmissionInstant())

        // All parcels of the same job have same geoServer url and parameter
        .geoServerUrl(parcel0.getGeoServerUrl().toString())
        .geoServerParameter(parcel0.getGeoServerParameter())
        .emailReceiver(domain.getEmailReceiver())
        .status(statusMapper.statusConverter(domain.getStatus()))
        .features(domain.getTasks().stream().map(featureMapper::fromZoneTilingTask).toList());
  }
}
