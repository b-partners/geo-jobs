package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.model.geometry.plot.AreImagesEqual;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class DetectionMaskCreatorTest {
  AreImagesEqual areImagesEqual = new AreImagesEqual(0.00005);
  DetectionMaskCreator subject = new DetectionMaskCreator();

  @Test
  void draw_mask_from_tile() throws IOException {}

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
