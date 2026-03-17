package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.model.AreaRate;
import app.bpartners.geojobs.service.RateComputerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AreaRateComputerController {
  private final RateComputerService service;

  @GetMapping("/area/rate")
  public AreaRate getAreaRate(
      @RequestParam double humiditeRate,
      @RequestParam double usureRate,
      @RequestParam double moisissureRate) {
    return service.computeRate(humiditeRate, usureRate, moisissureRate);
  }
}
