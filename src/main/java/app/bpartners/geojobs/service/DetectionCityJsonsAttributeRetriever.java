package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.CityJSON;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionCityJsonsAttributeRetriever implements Function<Detection, List<CityJSON>> {
  private final BucketComponent bucketComponent;

  @Override
  public List<CityJSON> apply(Detection detection) {
    List<app.bpartners.geojobs.repository.model.cityjson.CityJSON> cityJsons =
        detection.getCityJsons() == null ? List.of() : detection.getCityJsons();
    return cityJsons.parallelStream()
        .map(
            cityJson -> {
              var fileUrl = bucketComponent.presign(cityJson.getS3FileKey());
              return new CityJSON().id(cityJson.getId()).url(fileUrl);
            })
        .toList();
  }
}
