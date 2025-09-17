package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum.ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum.ZONE;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

class DetectionDelimitationRetrieverTest {
  GeometryConverter geometryConverterMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  DetectionDelimitationRetriever subject =
      new DetectionDelimitationRetriever(geometryConverterMock, detectionRepositoryMock);

  @Test
  void feature_with_delimitation_from_provided_geoJson_when_geoJsonDelimitation_type_is_roof()
      throws JsonProcessingException {
    var providedGeoJson = oneFeature();
    var detection = detection(List.of(providedGeoJson), ROOF);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);

    subject.accept(detection);

    var expectedDetectionToBeSaved =
        detection.toBuilder()
            .pointDelimitation(
                new HashMap<>(
                    Map.of(
                        new ObjectMapper().writeValueAsString(providedGeoJson), providedGeoJson)))
            .featureWithDelimitations(
                List.of(new FeatureWithDelimitation(providedGeoJson, List.of(providedGeoJson))))
            .build();

    verify(detectionRepositoryMock, times(1)).save(expectedDetectionToBeSaved);
  }

  @Test
  void building_api_should_be_called_when_geoJsonDelimitation_type_is_zone() {
    var providedGeoJson = oneFeature();
    var detection = detection(List.of(providedGeoJson), ZONE);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);
    when(geometryConverterMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.accept(detection);

    verify(geometryConverterMock, times(1)).retrieveRoofPolygonsFrom(any());
  }

  @Test
  void building_api_should_be_called_when_geoJsonDelimitation_type_is_null() {
    var providedGeoJson = oneFeature();
    var detection = detection(List.of(providedGeoJson), null);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);
    when(geometryConverterMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.accept(detection);

    verify(geometryConverterMock, times(1)).retrieveRoofPolygonsFrom(any());
  }

  private static Detection detection(
      List<Feature> providedGeoJson, GeoJsonDelimitationTypeEnum delimitationType) {
    return Detection.builder()
        .id("detectionId")
        .detectableObjectModel(new DetectableObjectModel().modelName(ModelName.TOITURE))
        .providedGeoJsonZone(providedGeoJson)
        .geoJsonDelimitationType(delimitationType)
        .build();
  }

  private static Feature oneFeature() {
    return Feature.builder()
        .id(randomUUID().toString())
        .zoom(20)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(Geometry.TypeEnum.POLYGON)
                .actualInstanceStringValue(
                    """
                    {
                      "type": "Polygon",
                      "coordinates": [
                        [
                          [0.0, 0.0],
                          [10.0, 0.0],
                          [5.0, 10.0],
                          [0.0, 0.0]
                        ]
                      ]
                    }
                    """)
                .build())
        .build();
  }
}
