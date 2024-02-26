package app.bpartners.geojobs.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  public static final String ANNOTATOR_USER_ID_FOR_GEOJOBS = "geo-jobs_user_id";

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("annotator.api.url", () -> "http://dummy.com");
    registry.add("tiles.downloader.mock", () -> "false");
    registry.add(
        "tiles.downloader.api.url",
        () -> "https://p36gjgisjpttscydepu3suuupi0wkute.lambda-url.eu-west-3.on.aws/");
    registry.add(
        "tile.detection.api.url",
        () -> "https://3gl56b3gqs6cfynuzqkte2rthm0onaoh.lambda-url.eu-west-3.on.aws/");
    registry.add("admin.api.key", () -> "the-admin-api-key");
    registry.add("annotator.api.key", () -> "the-admin-api-key");
    registry.add(
        "annotator.geojobs.user.info",
        () ->
            "{\"userId\":\""
                + ANNOTATOR_USER_ID_FOR_GEOJOBS
                + "\", \"teamId\":\"geo_jobs_team_id\"}");
    registry.add("jobs.status.update.retry.max.attempt", () -> 0);
  }
}
