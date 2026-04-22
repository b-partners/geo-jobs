package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;
import static app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStep.RAW_DELIMITATION_EXTRACTION;

import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPointsItem;
import app.bpartners.geojobs.model.lidar.planes.postprocessing.RoofFaceToLidarAlignmentFixer;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Plane3DGeneratorWithoutSegmentations implements Function<DelimitedRoofPoints, List<Plane3D>> {
  private final Plane3DExtractionStepExporter exporter;
  private final RoofFaceToLidarAlignmentFixer alignmentFixer;

  public Plane3DGeneratorWithoutSegmentations(Plane3DExtractorConf conf, Plane3DExtractionStepExporter exporter) {
    this.exporter = exporter;
    this.alignmentFixer = new RoofFaceToLidarAlignmentFixer(conf);
  }

  public Plane3D apply(DelimitedRoofPointsItem item) {
    var plane = getBestPlane(item);
    var delimitation = project(plane, item.getPolygon());
    return plane.toBuilder().delimitation(delimitation).build();
  }

  @Override
  public List<Plane3D> apply(DelimitedRoofPoints delimitedPoints) {
    var rawPlanes = Arrays.stream(delimitedPoints.getItems()).map(this::apply).toList();
    var aligned = alignmentFixer.apply(delimitedPoints, rawPlanes);
    for (int i = 0; i < aligned.size(); i++) {
      var plane = aligned.get(i);
      var subExporter = exporter.subSuffix(String.valueOf(i));
      subExporter.export(RAW_DELIMITATION_EXTRACTION, plane.getDelimitation());
    }
    return aligned;
  }

  static Plane3D getBestPlane(DelimitedRoofPointsItem item) {
    var conf = Plane3DExtractorConf.getDefault().toBuilder().doSkinnyArmRemover(false).build();
    var extractor = new OnePlane3DExtractor(conf);
    return extractor.apply(item.getPoints(), null).plane();
  }
}
