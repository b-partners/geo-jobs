package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ParcelizationControllerIT extends FacadeIT {
  @Autowired ParcelizationController subject;

  public static Feature expectedFeature() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(
                        BigDecimal.valueOf(47.530304168682925),
                        BigDecimal.valueOf(-18.863332120399996)),
                    List.of(
                        BigDecimal.valueOf(47.527370898166588),
                        BigDecimal.valueOf(-18.876071533552416)),
                    List.of(
                        BigDecimal.valueOf(47.546850822877644),
                        BigDecimal.valueOf(-18.876213867894698)),
                    List.of(
                        BigDecimal.valueOf(47.548505488297103),
                        BigDecimal.valueOf(-18.863332120399996)),
                    List.of(
                        BigDecimal.valueOf(47.530304168682925),
                        BigDecimal.valueOf(-18.863332120399996)))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(coordinates);
    feature.setGeometry(multiPolygon);
    return feature;
  }

  @Test
  void parcelization_with_default_params_ok() {
    List<Feature> features =
        subject.processFeatureParcelization(List.of(expectedFeature()), null, null, null);

    assertNotNull(features);
    assertEquals(2, features.size());
  }

  @Test
  void parcelization_with_params_ok() {
    List<Feature> featuresWithAllParams =
        subject.processFeatureParcelization(List.of(expectedFeature()), 20, 20, 0.00002);
    List<Feature> featuresWithZoomRef =
        subject.processFeatureParcelization(List.of(expectedFeature()), 20, null, null);

    assertNotNull(featuresWithAllParams);
    assertEquals(15, featuresWithAllParams.size());
    assertEquals(2, featuresWithZoomRef.size());
  }

  @Test
  void parcelization_ko() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          List<Feature> features = List.of(new Feature());
          subject.processFeatureParcelization(features, null, null, null);
        });
  }
}
