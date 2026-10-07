package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.lidar.planes.postprocessing.PanelVerticesMerger.withExteriorRing;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import java.util.function.UnaryOperator;
import org.locationtech.jts.geom.Coordinate;

/**
 * Merges in XYZ the vertices of neighbour panels closer than the threshold. A panel that would not
 * be valid anymore keeps its delimitation.
 */
public class Snapping3DComputer implements UnaryOperator<List<Plane3D>> {
  private final PanelVerticesMerger merger;

  public Snapping3DComputer(double threshold) {
    this.merger = new PanelVerticesMerger(threshold, Coordinate::distance3D);
  }

  @Override
  public List<Plane3D> apply(List<Plane3D> planes) {
    var merged = merger.apply(planes.stream().map(Plane3D::getDelimitation).toList());

    List<Plane3D> result = new ArrayList<>();
    for (int i = 0; i < planes.size(); i++) {
      var plane = planes.get(i);
      var delimitation = withExteriorRing(plane.getDelimitation(), merged.get(i));
      result.add(plane.toBuilder().delimitation(delimitation).build());
    }
    return result;
  }
}
