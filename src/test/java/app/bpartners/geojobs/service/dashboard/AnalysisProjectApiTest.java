package app.bpartners.geojobs.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AnalysisProjectApiTest {
  static final String BASE_URL = "https://dashboard.example.com";

  @Test
  void endpoint_of_a_project_step() {
    var actual = AnalysisProjectApi.endpoint(BASE_URL, "user-id", "v1:abc123", "2d");

    assertEquals(
        "https://dashboard.example.com/users/user-id/analysisProjects/v1:abc123/2d",
        actual.toString());
  }

  @Test
  void endpoint_of_a_project_without_step() {
    var actual = AnalysisProjectApi.endpoint(BASE_URL, "user-id", "city-json:request", null);

    assertEquals(
        "https://dashboard.example.com/users/user-id/analysisProjects/city-json:request",
        actual.toString());
  }

  @Test
  void project_key_cannot_add_path_segments() {
    var actual = AnalysisProjectApi.endpoint(BASE_URL, "user-id", "a/../../other", "3d");

    assertEquals(
        "https://dashboard.example.com/users/user-id/analysisProjects/a%2F..%2F..%2Fother/3d",
        actual.toString());
  }

  @Test
  void project_key_cannot_alter_the_query() {
    var actual = AnalysisProjectApi.endpoint(BASE_URL, "user-id", "key?admin=true#frag", null);

    assertEquals(
        "https://dashboard.example.com/users/user-id/analysisProjects/key%3Fadmin=true%23frag",
        actual.toString());
  }

  @Test
  void project_key_cannot_be_a_relative_path_segment() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AnalysisProjectApi.endpoint(BASE_URL, "user-id", "..", "2d"));
    assertThrows(
        IllegalArgumentException.class,
        () -> AnalysisProjectApi.endpoint(BASE_URL, "user-id", ".", "2d"));
  }

  @Test
  void project_key_cannot_be_blank() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AnalysisProjectApi.endpoint(BASE_URL, "user-id", " ", "2d"));
  }
}
