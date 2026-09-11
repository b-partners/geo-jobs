package app.bpartners.geojobs.service.lidar.api;

import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class IgnBrowserScraperLidarApi implements LidarApi {
  private final IgnBrowserScraperLidarApiConf conf;
  private final RestTemplate restTemplate;

  @Override
  public Set<String> apply(Envelope envelope) {
    var uriBuilder = UriComponentsBuilder.fromHttpUrl(conf.getUrl());
    conf.getDefaultParams(envelope).forEach(uriBuilder::queryParam);

    JsonNode data = restTemplate.getForObject(uriBuilder.toUriString(), JsonNode.class);

    if (data == null || !(data.get("tiles") instanceof ArrayNode tiles)) {
      log.warn("No tiles found from IGN Browser Scraper for bbox={}", uriBuilder.toUriString());
      return Set.of();
    }

    return StreamSupport.stream(tiles.spliterator(), false)
        .map(tile -> tile.get("url"))
        .filter(urlNode -> urlNode != null && !urlNode.isNull())
        .map(JsonNode::asText)
        .collect(toSet());
  }
}
