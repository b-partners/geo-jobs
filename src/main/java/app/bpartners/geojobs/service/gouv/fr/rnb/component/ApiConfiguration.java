package app.bpartners.geojobs.service.gouv.fr.rnb.component;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ApiConfiguration {
  private final String rnbApiUrl = "https://rnb-api.beta.gouv.fr";
}
