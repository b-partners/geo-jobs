package app.bpartners.geojobs.model.lidar.api;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class LidarApiUrlResolverConf {
  private final IgnLidarApi ignLidarApi;
  private final OpenSourceLidarApi openSourceLidarApi;
  private final SwissLidarApi swissLidarApi;

  @Bean
  public LidarApiUrlResolver lidarApiUrlResolver() {
    return new LidarApiUrlResolver(ignLidarApi, openSourceLidarApi, swissLidarApi);
  }
}
