package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.*;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.PlanesPostProcessingProcessor;
import java.util.*;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.locationtech.jts.geom.Polygon;

@Builder(toBuilder = true)
@AllArgsConstructor
public class Planes3DExtractor implements Function<Collection<LasPointGeometry>, List<Plane3D>> {
  private final Polygon roofDelimitation;
  private final Plane3DExtractorConf conf;
  private final Plane3DExtractionStepExporter exporter;
  private final OnePlane3DExtractor onePlane3DExtractor;

  public Planes3DExtractor(Polygon roofDelimitation, Plane3DExtractorConf conf) {
    this(roofDelimitation, conf, null);
  }

  public Planes3DExtractor(
      Polygon roofDelimitation, Plane3DExtractorConf conf, Plane3DExtractionStepExporter exporter) {
    this.conf = conf;
    this.exporter = exporter;
    this.roofDelimitation = roofDelimitation;
    this.onePlane3DExtractor = new OnePlane3DExtractor(conf);
  }

  @Override
  public List<Plane3D> apply(Collection<LasPointGeometry> points) {
    List<Plane3D> planes = new ArrayList<>();
    Set<LasPointGeometry> pointsToProcess = new HashSet<>(points);

    var doExport = exporter != null;
    if (doExport) {
      exporter.export(INIT_POINTS, points);
    }

    int i = 0;
    var minPointsCount = conf.planeConf().minPointsCount();
    while (pointsToProcess.size() > minPointsCount) {
      var result = onePlane3DExtractor.apply(pointsToProcess);
      var newPlane = result.plane();

      if (newPlane.getPoints().size() < minPointsCount) {
        break;
      }

      if (doExport) {
        var subExporter = exporter.subSuffix(String.valueOf(++i));
        subExporter.export(RAW_PLANE_KERNEL, result.plane().getKernel().getChains().getPoints());
        subExporter.export(RAW_PLANE_EXTRACTION, result.plane().getPoints());
        subExporter.export(ITERATION_POINTS_EVOLUTION, pointsToProcess);
        newPlane = newPlane.toBuilder().exporter(subExporter).build();
      }

      planes.add(newPlane);
      pointsToProcess = result.outliers();
    }

    return new PlanesPostProcessingProcessor(roofDelimitation, conf, points, exporter)
        .apply(planes);
  }
}
