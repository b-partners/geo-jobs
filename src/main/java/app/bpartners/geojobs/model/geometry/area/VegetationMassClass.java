package app.bpartners.geojobs.model.geometry.area;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VegetationMassClass {
  TRES_FAIBLE(null, 50.0),
  FAIBLE(50.0, 200.0),
  MOYEN(200.0, 500.0),
  FORTE(500.0, null);

  private final Double minArea; // exclusive
  private final Double maxArea; // inclusive

  public static VegetationMassClass fromArea(Double area) {
    return Arrays.stream(values()).filter(c -> c.apply(area)).findFirst().orElse(null);
  }

  public boolean apply(Double area) {
    if (minArea == null) {
      return area <= maxArea;
    } else if (maxArea == null) {
      return minArea < area;
    } else {
      return minArea < area && area <= maxArea;
    }
  }
}
