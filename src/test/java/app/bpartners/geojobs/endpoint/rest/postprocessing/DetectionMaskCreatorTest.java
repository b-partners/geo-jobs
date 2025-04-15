package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class DetectionMaskCreatorTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  DetectionMaskCreator subject = new DetectionMaskCreator();

  @Test
  void draw_mask_from_tile() throws IOException {
    var actual = subject.apply(List.of(feature()));
    var expectedTopLeft =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/topLeft.png"));
    var expectedTopCenter =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/topCenter.png"));
    var expectedTopRight =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/topRight.png"));
    var expectedCenterLeft =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/centerLeft.png"));
    var expectedCenterCenter =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/centerCenter.png"));
    var expectedCenterRight =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/centerRight.png"));
    var expectedBottomLeft =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/bottomLeft.png"));
    var expectedBottomCenter =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/bottomCenter.png"));
    var expectedBottomRight =
        ImageIO.read(this.getClass().getResourceAsStream("/geometry/bottomRight.png"));

    var topLeft = ImageIO.read(actual.get(new IntXY(1061594, 720906)));
    var topCenter = ImageIO.read(actual.get(new IntXY(1061595, 720906)));
    var topRight = ImageIO.read(actual.get(new IntXY(1061596, 720906)));

    var centerLeft = ImageIO.read(actual.get(new IntXY(1061594, 720907)));
    var centerCenter = ImageIO.read(actual.get(new IntXY(1061595, 720907)));
    var centerRight = ImageIO.read(actual.get(new IntXY(1061596, 720907)));

    var bottomLeft = ImageIO.read(actual.get(new IntXY(1061594, 720908)));
    var bottomCenter = ImageIO.read(actual.get(new IntXY(1061595, 720908)));
    var bottomRight = ImageIO.read(actual.get(new IntXY(1061596, 720908)));

    assertEquals(9, actual.size());
    assertTrue(areImagesEqual.apply(expectedTopLeft, topLeft));
    assertTrue(areImagesEqual.apply(expectedTopCenter, topCenter));
    assertTrue(areImagesEqual.apply(expectedTopRight, topRight));
    assertTrue(areImagesEqual.apply(expectedCenterLeft, centerLeft));
    assertTrue(areImagesEqual.apply(expectedCenterCenter, centerCenter));
    assertTrue(areImagesEqual.apply(expectedCenterRight, centerRight));
    assertTrue(areImagesEqual.apply(expectedBottomLeft, bottomLeft));
    assertTrue(areImagesEqual.apply(expectedBottomCenter, bottomCenter));
    assertTrue(areImagesEqual.apply(expectedBottomRight, bottomRight));
  }

  @SneakyThrows
  private Feature feature() {
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(new BigDecimal("2.234965"), new BigDecimal("48.921228")),
                    List.of(new BigDecimal("2.235011"), new BigDecimal("48.921158")),
                    List.of(new BigDecimal("2.235139"), new BigDecimal("48.921194")),
                    List.of(new BigDecimal("2.235115"), new BigDecimal("48.921229")),
                    List.of(new BigDecimal("2.235094"), new BigDecimal("48.921222")),
                    List.of(new BigDecimal("2.23507"), new BigDecimal("48.921257")),
                    List.of(new BigDecimal("2.235055"), new BigDecimal("48.921254")),
                    List.of(new BigDecimal("2.234965"), new BigDecimal("48.921228")),
                    List.of(new BigDecimal("2.234965"), new BigDecimal("48.921228")))));

    return toRestFeature(
        app.bpartners.geojobs.repository.model.Feature.builder()
            .id(null)
            .zoom(21)
            .geometry(
                app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                    .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                    .actualInstanceStringValue(
                        objectMapper()
                            .writeValueAsString(
                                new MultiPolygon()
                                    .coordinates(coordinates)
                                    .type(MultiPolygon.TypeEnum.MULTI_POLYGON)))
                    .build())
            .build());
  }
}
