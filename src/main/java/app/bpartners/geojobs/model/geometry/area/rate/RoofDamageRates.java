package app.bpartners.geojobs.model.geometry.area.rate;

import app.bpartners.geojobs.repository.model.detection.RoofCoveringType;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;

/**
 * TODO: vegetationFeu est pour l'instant une entree explicite du contrat ; une fois le module de
 * detection de vegetation/risque feu jugee fiable, sa valeur par defaut pourra en etre derivee
 * plutot que de rester null (a revoir).
 */
public record RoofDamageRates(
    Double humiditeRate,
    Double usureRate,
    Double moisissureRate,
    RoofCoveringType revetement,
    MutationType monitoring,
    Double penteDegres,
    Boolean vegetationFeu) {

  public RoofDamageRates(Double humiditeRate, Double usureRate, Double moisissureRate) {
    this(humiditeRate, usureRate, moisissureRate, null, null, null, null);
  }
}
