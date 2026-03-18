package app.bpartners.geojobs.model.geometry.area;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofScoreComputer {
  public double getGlobalRate(RoofCondition roofCondition) {
    return roofCondition.humiditeRate() * HumiditeAreaRateComputer.WEIGHT
        + roofCondition.usureRate() * UsureAreaRateComputer.WEIGHT
        + roofCondition.moisissureRate() * MoisissureAreaRateComputer.WEIGHT;
  }

  public Rate getRate(double globalRate) {
    if (globalRate < 4) {
      return Rate.A;
    }
    if (globalRate >= 4 && globalRate < 11) {
      return Rate.B;
    }
    if (globalRate >= 11 && globalRate < 21) {
      return Rate.C;
    }
    if (globalRate >= 21 && globalRate < 41) {
      return Rate.D;
    }
    return Rate.E;
  }
}
