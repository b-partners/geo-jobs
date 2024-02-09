package app.bpartners.geojobs.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class AnnotatorApiConf {
  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("annotator.api.url", () -> "http://dummy.com");
  }
}
