package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.utils.FeatureCreator;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class DetectionUpdateValidatorTest {
  DetectionUpdateValidator subject = new DetectionUpdateValidator();
  FeatureCreator featureCreator = new FeatureCreator();

  @Test
  void do_nothing_with_same_attributes() {
    List<Feature> geoJsonZone = featureCreator.defaultFeatures();
    GeoServerProperties geoServerProperties = new GeoServerProperties();

    assertDoesNotThrow(
        () ->
            subject.accept(
                new Detection(),
                new CreateDetection()
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel(new BPLomModel()))
                    .geoJsonZone(geoJsonZone)));
    assertDoesNotThrow(
        () ->
            subject.accept(
                Detection.builder()
                    .geoJsonZone(geoJsonZone)
                    .geoServerProperties(geoServerProperties)
                    .bpLomModel(new BPLomModel())
                    .build(),
                new CreateDetection()
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel(new BPLomModel()))
                    .geoJsonZone(geoJsonZone)));
    assertDoesNotThrow(
        () ->
            subject.accept(
                Detection.builder()
                    .geoJsonZone(geoJsonZone)
                    .geoServerProperties(geoServerProperties)
                    .bpToitureModel(new BPToitureModel())
                    .build(),
                new CreateDetection()
                    .geoServerProperties(geoServerProperties)
                    .detectableObjectModel(new DetectableObjectModel(new BPToitureModel()))
                    .geoJsonZone(geoJsonZone)));
  }

  @Test
  void throws_exception_with_updated_attributes() {
    List<Feature> geoJsonZone = featureCreator.defaultFeatures();
    GeoServerProperties geoServerProperties = new GeoServerProperties();

    var geoServerAndBPLomModelUpdateAttemptException =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                subject.accept(
                    Detection.builder()
                        .geoJsonZone(geoJsonZone)
                        .geoServerProperties(geoServerProperties)
                        .bpLomModel(new BPLomModel())
                        .build(),
                    new CreateDetection()
                        .geoServerProperties(new GeoServerProperties().geoServerUrl("dummyUrl"))
                        .detectableObjectModel(new DetectableObjectModel(new BPToitureModel()))
                        .geoJsonZone(null)));
    var geoJsonAndBPToitureModelUpdateAttemptException =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                subject.accept(
                    Detection.builder()
                        .geoJsonZone(List.of(new Feature()))
                        .geoServerProperties(geoServerProperties)
                        .bpToitureModel(new BPToitureModel())
                        .build(),
                    new CreateDetection()
                        .geoServerProperties(new GeoServerProperties())
                        .detectableObjectModel(new DetectableObjectModel(new BPLomModel()))
                        .geoJsonZone(null)));

    assertEquals(
        expectedGeoServerAndBPLomModelUpdateAttemptException(),
        geoServerAndBPLomModelUpdateAttemptException.getMessage());
    assertEquals(
        expectedGeoJsonAndBPToitureModelUpdateAttemptException(),
        geoJsonAndBPToitureModelUpdateAttemptException.getMessage());
  }

  @NonNull
  private String expectedGeoJsonAndBPToitureModelUpdateAttemptException() {
    return """
Detection.geoJsonZone can not be updated once it has values, otherwise actual value [class Feature {
    id: null
    zoom: null
    geometry: null
}] is not equals provided value null. Detection.detectableObjectModel can not be updated once it has values, otherwise actual value class class app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel {
    instance: class BPToitureModel {
        modelName: null
        toitureRevetement: true
        arbre: true
        velux: true
        panneauPhotovoltaique: true
        moisissure: true
        usure: true
        fissureCassure: true
        obstacle: true
        cheminee: null
        humidite: true
        risqueFeu: true
    }
    isNullable: false
    schemaType: oneOf
} is not equals provided value class class app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel {
    instance: class BPLomModel {
        modelName: null
        passagePieton: null
        voieCarrosable: null
        trottoir: null
        parking: null
    }
    isNullable: false
    schemaType: oneOf
}.\s""";
  }

  @NonNull
  private String expectedGeoServerAndBPLomModelUpdateAttemptException() {
    return """
Detection.geoJsonZone can not be updated once it has values, otherwise actual value [class Feature {
    id: feature_1_id
    zoom: null
    geometry: class MultiPolygon {
        coordinates: [[[[4.459648282829194, 45.904988912620688], [4.464709510872551, 45.928950368349426], [4.490816965688656, 45.941784543770964], [4.510354299995861, 45.933697132664598], [4.518386257467152, 45.912888345521047], [4.496344031095243, 45.883438201401809], [4.479593950305621, 45.882900828315755], [4.459648282829194, 45.904988912620688]]]]
        type: MultiPolygon
    }
}] is not equals provided value null. Detection.detectableObjectModel can not be updated once it has values, otherwise actual value class class app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel {
    instance: class BPLomModel {
        modelName: null
        passagePieton: null
        voieCarrosable: null
        trottoir: null
        parking: null
    }
    isNullable: false
    schemaType: oneOf
} is not equals provided value class class app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel {
    instance: class BPToitureModel {
        modelName: null
        toitureRevetement: true
        arbre: true
        velux: true
        panneauPhotovoltaique: true
        moisissure: true
        usure: true
        fissureCassure: true
        obstacle: true
        cheminee: null
        humidite: true
        risqueFeu: true
    }
    isNullable: false
    schemaType: oneOf
}. Detection.geoServerProperties can not be updated once it has values, otherwise actual value class GeoServerProperties {
    geoServerUrl: null
    geoServerParameter: null
} is not equals provided value class GeoServerProperties {
    geoServerUrl: dummyUrl
    geoServerParameter: null
}.\s""";
  }
}
