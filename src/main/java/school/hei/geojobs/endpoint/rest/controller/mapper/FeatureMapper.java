package school.hei.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;

import java.net.URL;
import org.springframework.stereotype.Component;
import school.hei.geojobs.endpoint.rest.model.Feature;
import school.hei.geojobs.endpoint.rest.model.GeoServerParameter;
import school.hei.geojobs.repository.model.ZoneTilingTask;
import school.hei.geojobs.repository.model.geo.Parcel;

@Component
public class FeatureMapper {
  public Parcel toDomain(Feature rest, URL geoServerUrl, GeoServerParameter GeoServerParameter) {
    return Parcel.builder()
        .id(rest.getId())
        .feature(rest)
        .geoServerUrl(geoServerUrl)
        .geoServerParameter(GeoServerParameter)
        .creationDatetime(String.valueOf(now()))
        .build();
  }

  public Feature fromZoneTilingTask(ZoneTilingTask domainTask) {
    return domainTask.getParcel().getFeature();
  }
}
