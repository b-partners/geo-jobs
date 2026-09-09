package app.bpartners.geojobs.service.lidar.api;

import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgnBrowserScraperLidarApiConf implements LidarApiConf {
  private final String url;

  public IgnBrowserScraperLidarApiConf(@Value("${ign-lidar.browser.scraper.api.url}") String url) {
    this.url = url;
  }

  @Override
  public Map<String, String> getDefaultParams(Envelope envelope) {
    String bbox =
        "%s,%s,%s,%s"
            .formatted(
                envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
    var params = new HashMap<String, String>();
    params.put("bbox", bbox);
    return params;
  }

  @Override
  public String getUrl() {
    return url;
  }
}
