package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.AreaRate;
import app.bpartners.geojobs.endpoint.rest.model.AreaRateClass;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.area.RateComputer;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class RateComputerService {

  public AreaRate computeRate(double humiditeRate, double usureRate, double moisissureRate) {
    validateRates(humiditeRate, usureRate, moisissureRate);
    var computer = new RateComputer(humiditeRate, usureRate, moisissureRate);
    var globalRate = BigDecimal.valueOf(computer.getGlobalRate());
    var rateClass = AreaRateClass.fromValue(computer.getRate().toString());

    return new AreaRate().globalRate(globalRate).rateClass(rateClass);
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
