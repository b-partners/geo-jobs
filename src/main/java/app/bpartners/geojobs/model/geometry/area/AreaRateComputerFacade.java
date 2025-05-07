package app.bpartners.geojobs.model.geometry.area;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import org.locationtech.jts.geom.Geometry;

public class AreaRateComputerFacade {
  private final HumiditeAreaRateComputer humiditeRateComputer;
  private final UsureAreaRateComputer usureRateComputer;
  private final MoisissureAreaRateComputer moisissureRateComputer;

  public AreaRateComputerFacade(Geometry roofGeometry, DetectedTile tile) {
    this.humiditeRateComputer = new HumiditeAreaRateComputer(roofGeometry.getArea(), tile);
    this.usureRateComputer = new UsureAreaRateComputer(roofGeometry.getArea(), tile);
    this.moisissureRateComputer = new MoisissureAreaRateComputer(roofGeometry.getArea(), tile);
  }

  private AreaRateComputer getComputer(DetectableType detectableType) {
    return switch (detectableType) {
      case HUMIDITE_CLAIR, HUMIDITE_INTENSE -> humiditeRateComputer;
      case USURE_IMPORTANTE, USURE_LEGER -> usureRateComputer;
      case MOISISSURE_CLAIR, MOISISSURE_NOIRCIE, MOISISSURE_COULEUR -> moisissureRateComputer;
      default ->
          throw new IllegalArgumentException("Unsupported detectable type: " + detectableType);
    };
  }

  public double compute(DetectableType detectableType) {
    return format(getComputer(detectableType).compute(detectableType) * 100);
  }

  public double getUsureAreaRate() {
    return format(usureRateComputer.getUsureAreaRate());
  }

  public double getMoisissureAreaRate() {
    return format(moisissureRateComputer.getMoisissureAreaRate());
  }

  public double getHumidityAreaRate() {
    return format(humiditeRateComputer.getHumidityAreaRate());
  }

  public double getGlobalRate() {
    return format(
        humiditeRateComputer.getGlobalRate()
            + usureRateComputer.getGlobalRate()
            + moisissureRateComputer.getGlobalRate());
  }

  public Rate getRate() {
    var globalRate = getGlobalRate();
    if (globalRate < 8) {
      return Rate.A;
    }
    if (globalRate >= 8 && globalRate < 20) {
      return Rate.A;
    }
    if (globalRate >= 20 && globalRate < 30) {
      return Rate.A;
    }
    if (globalRate >= 30 && globalRate < 40) {
      return Rate.A;
    }
    return Rate.E;
  }

  private double format(double value) {
    return Math.round((value * 100)) / 100.0;
  }
}
