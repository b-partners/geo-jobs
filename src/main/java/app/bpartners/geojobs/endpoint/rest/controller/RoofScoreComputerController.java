package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.RoofScoreMapper;
import app.bpartners.geojobs.endpoint.rest.model.RoofScore;
import app.bpartners.geojobs.endpoint.rest.validator.RoofConditionValidator;
import app.bpartners.geojobs.model.geometry.area.RoofDamageRates;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoofScoreComputerController {
  private final RoofScoreMapper mapper;
  private final RoofConditionValidator roofConditionValidator;

  @GetMapping("/roof/overall-score")
  public RoofScore computeRoofOverallScore(
      @RequestParam double humiditeRate,
      @RequestParam double usureRate,
      @RequestParam double moisissureRate) {
    RoofDamageRates roofDamageRates = new RoofDamageRates(humiditeRate, usureRate, moisissureRate);
    roofConditionValidator.accept(roofDamageRates);

    return mapper.from(roofDamageRates);
  }
}
