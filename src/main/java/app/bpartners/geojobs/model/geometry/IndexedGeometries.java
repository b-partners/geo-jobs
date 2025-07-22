package app.bpartners.geojobs.model.geometry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

public class IndexedGeometries {

  private final STRtree rtree = new STRtree();

  public IndexedGeometries(Set<Geometry> geometries) {
    for (var geometry : geometries) {
      rtree.insert(geometry.getEnvelopeInternal(), geometry);
    }
  }

  public Set<Geometry> containedIn(Geometry container) {
    Set<Geometry> res = new HashSet<>();

    // R-Tree uses Envelope as keys for Geometry values
    // We need to test whether candidate results are truly contained
    List<Geometry> containedCandidates = rtree.query(container.getEnvelopeInternal());
    for (var containedCandidate : containedCandidates) {
      if (container.contains(containedCandidate)) {
        res.add(containedCandidate);
      }
    }

    return res;
  }
}
