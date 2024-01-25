package app.bpartners.geojobs.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "tiles.downloader.api.url",
        () -> "https://hds4hpeacrrder7h32qy4lmy4a0wrewx.lambda-url.eu-west-3.on.aws");
    registry.add(
        "tile.detection.api.url",
        () -> "https://3gl56b3gqs6cfynuzqkte2rthm0onaoh.lambda-url.eu-west-3.on.aws");
  }
}
