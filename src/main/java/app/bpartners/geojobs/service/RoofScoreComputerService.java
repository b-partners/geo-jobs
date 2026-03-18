package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.area.RoofScore;
import app.bpartners.geojobs.model.geometry.area.RoofScoreComputer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoofScoreComputerService {
  private final RoofScoreComputer computer;

  public RoofScore computeScore(double humiditeRate, double usureRate, double moisissureRate) {
    validateRates(humiditeRate, usureRate, moisissureRate);
    var score = computer.getGlobalRate(humiditeRate, usureRate, moisissureRate);
    var roofScoreCategory = computer.getRate(humiditeRate, usureRate, moisissureRate);

    return new RoofScore(score, roofScoreCategory);
  }

  private void validateRates(double humiditeRate, double usureRate, double moisissureRate) {
    if (humiditeRate < 0 || usureRate < 0 || moisissureRate < 0) {
      throw new BadRequestException("Rates must be positive");
    }

    var sumRate = humiditeRate + usureRate + moisissureRate;
    if (sumRate > 100) {
      throw new BadRequestException("Sum of rates must not exceed 100, actual : " + sumRate);
    }
  }
}
