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

    var topLeft = ImageIO.read(actual.get(new IntXY(530798, 360453)));
    var topCenter = ImageIO.read(actual.get(new IntXY(530799, 360453)));
    var topRight = ImageIO.read(actual.get(new IntXY(530800, 360453)));
    var centerLeft = ImageIO.read(actual.get(new IntXY(530798, 360454)));
    var centerCenter = ImageIO.read(actual.get(new IntXY(530799, 360454)));
    var centerRight = ImageIO.read(actual.get(new IntXY(530800, 360454)));
    var bottomLeft = ImageIO.read(actual.get(new IntXY(530798, 360455)));
    var bottomCenter = ImageIO.read(actual.get(new IntXY(530799, 360455)));
    var bottomRight = ImageIO.read(actual.get(new IntXY(530800, 360455)));

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
                    List.of(
                        new BigDecimal("2.2351245768056716"), new BigDecimal("48.92126795344402")),
                    List.of(
                        new BigDecimal("2.2351695178681674"), new BigDecimal("48.92119716835145")),
                    List.of(
                        new BigDecimal("2.2353000609544655"), new BigDecimal("48.921232560897735")),
                    List.of(
                        new BigDecimal("2.235278660448515"), new BigDecimal("48.92126653774217")),
                    List.of(
                        new BigDecimal("2.235243706288796"), new BigDecimal("48.9212589873323")),
                    List.of(
                        new BigDecimal("2.2352165989812587"), new BigDecimal("48.921296267481054")),
                    List.of(
                        new BigDecimal("2.2351245768056716"),
                        new BigDecimal("48.92126795344402")))));
    return toRestFeature(
        app.bpartners.geojobs.repository.model.Feature.builder()
            .id(null)
            .zoom(20)
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
