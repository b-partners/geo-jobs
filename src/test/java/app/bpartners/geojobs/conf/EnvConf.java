package app.bpartners.geojobs.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("annotator.api.url", () -> "http://dummy.com");
    registry.add(
        "tiles.downloader.api.url",
        () -> "https://p36gjgisjpttscydepu3suuupi0wkute.lambda-url.eu-west-3.on.aws/");
    registry.add(
        "tile.detection.api.url",
        () -> "https://3gl56b3gqs6cfynuzqkte2rthm0onaoh.lambda-url.eu-west-3.on.aws/");
    registry.add("admin.api.key", () -> "the-admin-api-key");
  }
}
