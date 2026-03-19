package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.RoofScoreCategory.E;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.RoofScoreMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson.RoofScoreCategoryMapper;
import app.bpartners.geojobs.endpoint.rest.validator.RoofConditionValidator;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.area.RoofScoreComputer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RoofScoreComputerControllerTest {
  RoofScoreComputer scoreComputer = new RoofScoreComputer();
  RoofConditionValidator conditionValidator = new RoofConditionValidator();
  RoofScoreCategoryMapper categoryMapper = new RoofScoreCategoryMapper();
  RoofScoreMapper mapper = new RoofScoreMapper(categoryMapper, scoreComputer);
  RoofScoreComputerController subject = new RoofScoreComputerController(mapper, conditionValidator);

  @Test
  void rate_compute_ok() {
    var actual = subject.computeRoofOverallScore(10, 20, 30);

    assertEquals(BigDecimal.valueOf(42.0), actual.getScore());
    assertEquals(E, actual.getCategory());
  }

  @Test
  void negative_rate_ko() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.computeRoofOverallScore(-1, 20, 30));

    assertEquals("Rates must be positive", actual.getMessage());
  }

  @Test
  void sum_rate_ko() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.computeRoofOverallScore(100, 20, 40));

    assertEquals("Sum of rates must not exceed 100, actual : " + 160.0, actual.getMessage());
  }
}
