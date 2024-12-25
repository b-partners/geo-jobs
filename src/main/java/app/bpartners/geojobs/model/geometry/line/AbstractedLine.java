package app.bpartners.geojobs.model.geometry.line;

import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
public class AbstractedLine {
  private final Polygon line;
  private final AlphaConf alphaConf;
  private final Set<OrientedQuadrilateral> abstraction;

  public AbstractedLine(Polygon line, AlphaConf alphaConf) {
    this.line = line;
    this.alphaConf = alphaConf;
    this.abstraction = new Alpha(line, alphaConf).get();
  }
}
