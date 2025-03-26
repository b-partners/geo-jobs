package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.repository.model.Feature;
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
    var file = subject.apply(List.of(feature()));
    var expected = ImageIO.read(this.getClass().getResourceAsStream("/geometry/mask-test.png"));
    var actual = ImageIO.read(file);

    assertTrue(areImagesEqual.apply(expected, actual));
  }

  @SneakyThrows
  private Feature feature() {
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(
                        new BigDecimal("2.212835046198338"), new BigDecimal("48.82678284991411")),
                    List.of(
                        new BigDecimal("2.212352902647264"), new BigDecimal("48.826611613518985")),
                    List.of(
                        new BigDecimal("2.212543222470057"), new BigDecimal("48.826444553057378")),
                    List.of(
                        new BigDecimal("2.213076117973874"), new BigDecimal("48.826586554485246")),
                    List.of(
                        new BigDecimal("2.212835046198338"),
                        new BigDecimal("48.82678284991411")))));
    return Feature.builder()
        .id(null)
        .zoom(20)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                .actualInstanceStringValue(
                    objectMapper()
                        .writeValueAsString(
                            new MultiPolygon()
                                .coordinates(coordinates)
                                .type(MultiPolygon.TypeEnum.MULTI_POLYGON)))
                .build())
        .build();
  }
}
