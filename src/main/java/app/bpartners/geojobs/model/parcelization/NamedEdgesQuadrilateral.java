package app.bpartners.geojobs.model.parcelization;

import java.util.Set;
import lombok.Getter;
import org.locationtech.jts.geom.Coordinate;

/*
 * Edges are named after their lexicographic order over (latitude, longitude).
 * That is: edge of order 0 is named a, 1->b, 2->c, 3->d.
 *
 *   ^ latitude
 *   |
 *   |    b---------d
 *   |    |         |
 *   |    a---------c
 *   _______________________________> longitude
 */
@Getter
public class NamedEdgesQuadrilateral {
  private final Coordinate a;
  private final Coordinate b;
  private final Coordinate c;
  private final Coordinate d;

  public NamedEdgesQuadrilateral(Set<Coordinate> coordinates) {
    checkQuadrilateral(coordinates);
    var sorted =
        coordinates.stream()
            .sorted(
                Coordinate::compareTo) // Coordinate::compareTo is lexicographic order over (x,y)
            .toList();
    this.a = sorted.get(0);
    this.b = sorted.get(1);
    this.c = sorted.get(2);
    this.d = sorted.get(3);
  }

  private void checkQuadrilateral(Set<Coordinate> coordinates) {
    if (coordinates.size() != 4) {
      throw new IllegalArgumentException(
          "Quadrilateral expects 4 coordinates but got: " + coordinates);
    }
  }
}
