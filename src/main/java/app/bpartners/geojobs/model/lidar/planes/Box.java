package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Box {
  private final Plane3D plane;
  private final double threshold;
  private final List<LasPointGeometry> points;

  public Box(
      LasPointGeometry p1,
      LasPointGeometry p2,
      LasPointGeometry p3,
      double threshold,
      double planeDelimitationConcaveRatio,
      double planeDelimitationSimplificationEpsilon,
      Plane3DExtractionStepExporter exporter) {
    this.plane =
        Plane3D.fit(
            p1,
            p2,
            p3,
            planeDelimitationConcaveRatio,
            planeDelimitationSimplificationEpsilon,
            exporter);
    this.threshold = threshold;
    this.points = new ArrayList<>(List.of(p1, p2, p3));
  }

  public boolean contains(LasPointGeometry point) {
    return plane.distance(point) <= threshold;
  }

  public boolean add(LasPointGeometry point) {
    if (!contains(point)) {
      return false;
    }

    this.points.add(point);
    return true;
  }

  public List<LasPointGeometry> add(Collection<LasPointGeometry> points) {
    var insideBox = points.stream().filter(this::contains).toList();

    this.points.addAll(insideBox);
    return insideBox;
  }
}
