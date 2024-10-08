package app.bpartners.readme.monitor;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("readme.monitor")
@NoArgsConstructor
public class ReadmeMonitorConf {
  private String apiKey;
  private String version;
  private String url;
  private String name;
  private boolean development;
}
