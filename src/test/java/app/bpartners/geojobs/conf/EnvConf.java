package app.bpartners.geojobs.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  public static final String ANNOTATOR_USER_ID_FOR_GEOJOBS = "geo-jobs_user_id";

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("annotator.api.url", () -> "http://dummy.com");
    registry.add("tiles.downloader.mock.activated", () -> "false");
    registry.add("objects.detector.mock.activated", () -> "false");
    registry.add(
        "tiles.downloader.api.url",
        () -> "https://r7e7c5gzxuhzdvudjreormg4ja0afglo.lambda-url.eu-west-3.on.aws");
    registry.add(
        "tile.detection.api.urls",
        () ->
            "[ { \"objectType\": \"ROOF\", \"url\": \"https://roof-api.azurewebsites.net/api\" }, {"
                + " \"objectType\": \"PATHWAY\", \"url\":"
                + " \"https://2p3jsxl3zadpi7r5fysmq4mzsu0fzphw.lambda-url.eu-west-3.on.aws\" }, {"
                + " \"objectType\": \"SOLAR_PANEL\", \"url\":"
                + " \"https://solarpanel-api.azurewebsites.net/api\" }, { \"objectType\": \"POOL\","
                + " \"url\": \"https://pool-api.azurewebsites.net/api\" }, { \"objectType\":"
                + " \"TREE\", \"url\": \"https://trees-api.azurewebsites.net/api\" }, {"
                + " \"objectType\": \"SIDEWALK\", \"url\":"
                + " \"https://sidewalk-api.azurewebsites.net/api\" }, { \"objectType\": \"LINE\","
                + " \"url\": \"https://line-api.azurewebsites.net/api\" }, { \"objectType\":"
                + " \"GREEN_SPACE\", \"url\": \"https://greenspace-api.azurewebsites.net/api\" }"
                + " ]");
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
