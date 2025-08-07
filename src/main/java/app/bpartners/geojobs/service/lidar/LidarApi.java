package app.bpartners.geojobs.service.lidar;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.file.FileWriter;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@AllArgsConstructor
@Slf4j
public class LidarApi implements Function<Set<Envelope>, Set<File>> {
  private final LidarApiConf conf;
  private final RestTemplate restTemplate;
  private final FileWriter fileWriter;

  @Override
  public Set<File> apply(Set<Envelope> bboxes) {
    Set<String> uniqueUrls = new HashSet<>();

    for (var bbox : bboxes) {
      var uriBuilder = UriComponentsBuilder.fromHttpUrl(conf.getUrl());
      conf.getDefaultParams(bbox).forEach(uriBuilder::queryParam);

      var features =
          requireNonNull(
                  restTemplate
                      .getForEntity(uriBuilder.toUriString(), FeatureCollection.class)
                      .getBody())
              .getFeatures();

      for (var feature : features) {
        var url = feature.getProperties().get("url").toString();
        log.info("LAZ File to download: {}", url);
        uniqueUrls.add(url);
      }
    }

    return uniqueUrls.stream().map(this::downloadToTempFile).filter(Optional::isPresent).map(Optional::get).collect(toSet());
  }

  public Set<File> apply(List<Geometry> geometries) {
    var bboxes = geometries.stream().map(Geometry::getEnvelopeInternal).collect(toSet());
    return apply(bboxes);
  }

  private Optional<File> downloadToTempFile(String fileUrl) {
    try {
      var data = Optional.ofNullable(restTemplate.getForObject(fileUrl, byte[].class));
      return data.map(bytes -> fileWriter.apply(bytes, null));
    } catch (Exception e) {
      log.error("Failed to download LAZ file from {}", fileUrl, e);
      throw new RuntimeException("Could not download file: " + fileUrl, e);
    }
  }
}
