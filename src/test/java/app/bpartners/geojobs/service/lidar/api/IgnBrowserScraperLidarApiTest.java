package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.conf.EnvConf.IGN_BROWSER_SCRAPER_API_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

class IgnBrowserScraperLidarApiTest {
  RestTemplate restTemplateMock = mock();
  IgnBrowserScraperLidarApiConf conf =
      new IgnBrowserScraperLidarApiConf(IGN_BROWSER_SCRAPER_API_URL);
  IgnBrowserScraperLidarApi subject = new IgnBrowserScraperLidarApi(conf, restTemplateMock);

  private static final Envelope BBOX = new Envelope(2.391307, 2.392127, 48.865264, 48.865804);

  @Test
  @SneakyThrows
  void builds_bbox_query_param_as_comma_joined_wgs84_coordinates() {
    when(restTemplateMock.getForObject(any(String.class), eq(JsonNode.class)))
        .thenReturn(emptyResponse());
    var urlCaptor = ArgumentCaptor.forClass(String.class);

    subject.apply(BBOX);

    verify(restTemplateMock).getForObject(urlCaptor.capture(), eq(JsonNode.class));
    assertTrue(urlCaptor.getValue().contains("bbox=2.391307,48.865264,2.392127,48.865804"));
  }

  @Test
  void returns_urls_found_in_tiles() {
    when(restTemplateMock.getForObject(any(String.class), eq(JsonNode.class)))
        .thenReturn(responseWithTiles());

    var actual = subject.apply(BBOX);

    assertEquals(
        Set.of(
            "https://data.geopf.fr/telechargement/download/LiDARHD-NUALID/NUALHD_1-0__LAZ_LAMB93_KE_2025-06-06/LHD_FXX_0655_6864_PTS_LAMB93_IGN69.copc.laz",
            "https://data.geopf.fr/telechargement/download/LiDARHD-NUALID/NUALHD_1-0__LAZ_LAMB93_KE_2025-06-06/LHD_FXX_0655_6863_PTS_LAMB93_IGN69.copc.laz"),
        actual);
  }

  @Test
  void returns_empty_set_when_no_tiles_found() {
    when(restTemplateMock.getForObject(any(String.class), eq(JsonNode.class)))
        .thenReturn(emptyResponse());

    var actual = subject.apply(BBOX);

    assertTrue(actual.isEmpty());
  }

  @Test
  void returns_empty_set_when_response_is_null() {
    when(restTemplateMock.getForObject(any(String.class), eq(JsonNode.class))).thenReturn(null);

    var actual = subject.apply(BBOX);

    assertTrue(actual.isEmpty());
  }

  @SneakyThrows
  private static JsonNode responseWithTiles() {
    return new ObjectMapper()
        .readTree(
            """
{
  "bbox": [2.391307, 48.865264, 2.392127, 48.865804],
  "count": 2,
  "tiles": [
    {
      "name": "LHD_FXX_0655_6864_PTS_O_LAMB93_IGN69",
      "url": "https://data.geopf.fr/telechargement/download/LiDARHD-NUALID/NUALHD_1-0__LAZ_LAMB93_KE_2025-06-06/LHD_FXX_0655_6864_PTS_LAMB93_IGN69.copc.laz"
    },
    {
      "name": "LHD_FXX_0655_6863_PTS_O_LAMB93_IGN69",
      "url": "https://data.geopf.fr/telechargement/download/LiDARHD-NUALID/NUALHD_1-0__LAZ_LAMB93_KE_2025-06-06/LHD_FXX_0655_6863_PTS_LAMB93_IGN69.copc.laz"
    }
  ]
}
""");
  }

  @SneakyThrows
  private static JsonNode emptyResponse() {
    return new ObjectMapper()
        .readTree(
            """
            {
              "bbox": [2.391307, 48.865264, 2.392127, 48.865804],
              "count": 0,
              "tiles": []
            }
            """);
  }
}
