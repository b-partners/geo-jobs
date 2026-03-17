package app.bpartners.geojobs.model.geometry.area;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RateComputer {
  private final double humiditeRate;
  private final double usureRate;
  private final double moisissureRate;

  public double getGlobalRate() {
    return humiditeRate * HumiditeAreaRateComputer.WEIGHT
        + usureRate * UsureAreaRateComputer.WEIGHT
        + moisissureRate * MoisissureAreaRateComputer.WEIGHT;
  }

  public Rate getRate() {
    return getRate(getGlobalRate());
  }

  public static Rate getRate(double globalRate) {
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
