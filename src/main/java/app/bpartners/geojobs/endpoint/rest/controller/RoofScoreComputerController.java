package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.RoofScoreMapper;
import app.bpartners.geojobs.endpoint.rest.model.RoofScore;
import app.bpartners.geojobs.service.RoofScoreComputerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoofScoreComputerController {
  private final RoofScoreComputerService service;
  private final RoofScoreMapper mapper;

  @GetMapping("/roof/overall-score")
  public RoofScore computeRoofOverallScore(
      @RequestParam double humiditeRate,
      @RequestParam double usureRate,
      @RequestParam double moisissureRate) {
    return mapper.toRest(service.computeScore(humiditeRate, usureRate, moisissureRate));
  }
}
