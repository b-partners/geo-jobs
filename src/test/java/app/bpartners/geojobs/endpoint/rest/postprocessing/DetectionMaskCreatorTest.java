package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

public class DetectionMaskCreatorTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  DetectionMaskCreator subject = new DetectionMaskCreator();

  @Test
  void draw_mask_from_tile() throws IOException {
    var expected =
        ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/geometry/mask.png")));
    var coordinates =
        List.of(
            List.of(new BigDecimal("465.95744680851067"), new BigDecimal("282.97872340425533")),
            List.of(new BigDecimal("780.8510638297872"), new BigDecimal("421.2765957446809")),
            List.of(new BigDecimal("619.1489361702128"), new BigDecimal("800")),
            List.of(new BigDecimal("474.468085106383"), new BigDecimal("729.7872340425532")),
            List.of(new BigDecimal("510.63829787234044"), new BigDecimal("636.1702127659574")),
            List.of(new BigDecimal("351.06382978723406"), new BigDecimal("557.4468085106383")),
            List.of(new BigDecimal("465.95744680851067"), new BigDecimal("282.97872340425533")));
    var actual = subject.apply(coordinates);

    assertTrue(areImagesEqual.apply(expected, ImageIO.read(actual)));
  }
}
