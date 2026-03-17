package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.AreaRateClass.E;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.RateComputerService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AreaRateComputerControllerTest {
  RateComputerService service = new RateComputerService();
  AreaRateComputerController subject = new AreaRateComputerController(service);

  @Test
  void rate_compute_ok() {
    var actual = subject.getAreaRate(10, 20, 30);

    assertEquals(BigDecimal.valueOf(42.0), actual.getGlobalRate());
    assertEquals(E, actual.getRateClass());
  }

  @Test
  void negative_rate_ko() {
    var actual = assertThrows(BadRequestException.class, () -> subject.getAreaRate(-1, 20, 30));

    assertEquals("Rates must be positive", actual.getMessage());
  }

  @Test
  void sum_rate_ko() {
    var actual = assertThrows(BadRequestException.class, () -> subject.getAreaRate(100, 20, 40));

    assertEquals("Sum of rates must not exceed 100, actual : " + 160.0, actual.getMessage());
  }
}
