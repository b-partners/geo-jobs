package app.bpartners.geojobs.model.geometry.quadrilateral;

import app.bpartners.geojobs.model.geometry.polygon.EnvelopeAsPolygon;
import app.bpartners.geojobs.model.geometry.polygon.LongestInteriorLine;
import app.bpartners.geojobs.model.geometry.polygon.MaximalPolygonFromEdges;
import app.bpartners.geojobs.model.geometry.polygon.OrientedBoundingBox;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class SubAlpha implements Supplier<Quadrilateral> {

  private final Polygon p;

  @Override
  public Quadrilateral get() {
    var envelopeOfLil = new EnvelopeAsPolygon(new LongestInteriorLine(p).get()).get();
    var envelopeOfLil_inter_p = (Polygon) envelopeOfLil.intersection(p);
    var obb = new OrientedBoundingBox(envelopeOfLil_inter_p).get();

    var obb_inter_p = (Polygon) obb.intersection(p);
    return new Quadrilateral(new MaximalPolygonFromEdges(obb_inter_p, 4).get());
    // TODO(enhancement): minimize holes, if any, by moving inwards the edges of obtained
    // quadrilateral.
    //  Do it in a binary-search fashion until fixpoint is found.
  }
}
