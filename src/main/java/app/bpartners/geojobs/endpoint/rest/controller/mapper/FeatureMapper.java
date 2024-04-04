package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeatureMapper {

  public Parcel toDomain(
      String parcelId, Feature rest, URL geoServerUrl, GeoServerParameter GeoServerParameter) {
    Parcel extractedParcel =
        Parcel.builder()
            .id(parcelId)
            .parcelContent(
                ParcelContent.builder()
                    .id(rest.getId())
                    .feature(rest)
                    .geoServerUrl(geoServerUrl)
                    .geoServerParameter(GeoServerParameter)
                    .creationDatetime(now())
                    .build())
            .build();
    return extractedParcel;
  }

  public Feature from(TilingTask domainTask) {
    return domainTask.getParcelContent().getFeature();
  }
}
