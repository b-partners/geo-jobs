package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.area.RoofCondition;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class RoofConditionValidator implements Consumer<RoofCondition> {

  @Override
  public void accept(RoofCondition roofCondition) {
    double humiditeRate = roofCondition.humiditeRate();
    double usureRate = roofCondition.usureRate();
    double moisissureRate = roofCondition.moisissureRate();

    if (humiditeRate < 0 || usureRate < 0 || moisissureRate < 0) {
      throw new BadRequestException("Rates must be positive");
    }

    var sumRate = humiditeRate + usureRate + moisissureRate;
    if (sumRate > 100) {
      throw new BadRequestException("Sum of rates must not exceed 100, actual : " + sumRate);
    }
  }
}
