package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.service.cityjson.model.object.CityJsonIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class CityJsonIOTest {

  @SneakyThrows
  @Test
  void convert_string_to_city_model() {

    var file = new ClassPathResource("/cityjson/dummy.jsonl").getFile();

    var actual = CityJsonIO.computeAdditionalProperties(file);

    assertNotNull(
        actual
            .features
            .getFirst()
            .getCityObjects()
            .get("1-0")
            .getGeometry()
            .getFirst()
            .getSemantics()
            .getSurfaces());
    log.info(
        actual
            .features
            .getFirst()
            .getCityObjects()
            .get("1-0")
            .getGeometry()
            .getFirst()
            .getSemantics()
            .getSurfaces()
            .toString());
  }
}
