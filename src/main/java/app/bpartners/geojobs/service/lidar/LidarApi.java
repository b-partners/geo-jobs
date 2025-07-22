package app.bpartners.geojobs.service.lidar;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@AllArgsConstructor
@Slf4j
public class LidarApi implements Function<Envelope, Set<File>> {
  private final LidarApiConf conf;
  private final RestTemplate restTemplate;

  @Override
  public Set<File> apply(Envelope bbox) {
    var uriBuilder = UriComponentsBuilder.fromHttpUrl(conf.getUrl());
    conf.getDefaultParams(bbox).forEach(uriBuilder::queryParam);

    var lazFiles =
        Objects.requireNonNull(
                restTemplate
                    .getForEntity(uriBuilder.toUriString(), FeatureCollection.class)
                    .getBody())
            .getFeatures()
            .stream()
            .map(
                f -> {
                  var fileUrl = f.getProperties().get("url");
                  log.info("LAZ file url {}", fileUrl);
                  return restTemplate.getForEntity(fileUrl.toString(), File.class).getBody();
                })
            .collect(toSet());

    return lazFiles;
  }
}
