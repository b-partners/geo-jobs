package app.bpartners.geojobs.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  public static final String ANNOTATOR_USER_ID_FOR_GEOJOBS = "geo-jobs_user_id";
  public static final String ADMIN_EMAIL = "admin@gmail.com";

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
            "[ { \"objectType\": \"TOITURE_REVETEMENT\", \"url\":"
                + " \"https://roof-api.azurewebsites.net/api\" }, { \"objectType\":"
                + " \"PASSAGE_PIETON\", \"url\": \"https://pathway-api.azurewebsites.net/api\" }, {"
                + " \"objectType\": \"PANNEAU_PHOTOVOLTAIQUE\", \"url\":"
                + " \"https://solarpanel-api.azurewebsites.net/api\" }, { \"objectType\":"
                + " \"PISCINE\", \"url\": \"https://pool-api.azurewebsites.net/api\" }, {"
                + " \"objectType\": \"ARBRE\", \"url\": \"https://trees-api.azurewebsites.net/api\""
                + " }, { \"objectType\": \"TROTTOIR\", \"url\":"
                + " \"https://sidewalk-api.azurewebsites.net/api\" }, { \"objectType\": \"LINE\","
                + " \"url\": \"https://line-api.azurewebsites.net/api\" }, { \"objectType\":"
                + " \"ESPACE_VERT\", \"url\": \"https://greenspace-api.azurewebsites.net/api\" }"
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
    registry.add("admin.email", () -> ADMIN_EMAIL);
    registry.add("readme.monitor.url", () -> "https://dummy.com");
    registry.add("readme.monitor.api-key", () -> "the-readme-monitor-api-key");
    registry.add("readme.monitor.development", () -> "true");
  }
}
