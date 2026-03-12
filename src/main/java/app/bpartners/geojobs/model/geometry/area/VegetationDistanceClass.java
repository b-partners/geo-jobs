package app.bpartners.geojobs.model.geometry.area;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum VegetationDistanceClass {
  COLLEE_AU_BATI(null, 2.0),
  PROCHE(2.0, 5.0),
  ENVIRONNEMENT_PROCHE(5.0, 15.0),
  LOINTAINE(15.0, null);

  private final Double minDistance; // exclusive
  private final Double maxDistance; // inclusive

  static VegetationDistanceClass fromDistance(Double distance) {
    return Arrays.stream(values()).filter(c -> c.apply(distance)).findFirst().orElse(null);
  }

  public boolean apply(Double distance) {
    if (minDistance == null) {
      return distance <= maxDistance;
    } else if (maxDistance == null) {
      return minDistance < distance;
    } else {
      return minDistance < distance && distance <= maxDistance;
    }
  }
}
