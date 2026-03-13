package app.bpartners.geojobs;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConf {

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    var factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(10L));
    factory.setConnectionRequestTimeout(Duration.ofSeconds(120L));
    return builder.requestFactory(() -> factory).build();
  }
}
