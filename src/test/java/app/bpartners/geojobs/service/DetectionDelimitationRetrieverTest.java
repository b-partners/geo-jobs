package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ZONE;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.BuildingFootPrintSourceUnavailableException;
import app.bpartners.geojobs.model.geometry.RoofDetails;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.ign.IgnCadastreFeatureFetcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;

class DetectionDelimitationRetrieverTest {
  GeometryConverter geometryConverterMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ObjectMapper objectMapperMock = mock();
  IgnCadastreFeatureFetcher ignCadastreFeatureFetcher = mock();
  BuildingFinder buildingFinderMock = mock();
  DetectionDelimitationRetriever subject =
      new DetectionDelimitationRetriever(
          geometryConverterMock,
          detectionRepositoryMock,
          objectMapperMock,
          ignCadastreFeatureFetcher,
          buildingFinderMock);

  @SneakyThrows
  @Test
  void feature_with_delimitation_from_provided_geoJson_when_geoJsonDelimitation_type_is_roof()
      throws JsonProcessingException {
    var providedGeoJson = multiPolygonFeature();
    var detection = detection(List.of(providedGeoJson), ROOF);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);

    subject.apply(detection);

    var featureWithDelimitationAsString = featureWithGeometryAsString(providedGeoJson);
    var expectedDetectionToBeSaved =
        detection.toBuilder()
            .pointDelimitation(
                new HashMap<>(
                    Map.of(
                        new ObjectMapper().writeValueAsString(featureWithDelimitationAsString),
                        featureWithDelimitationAsString)))
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        providedGeoJson, List.of(featureWithDelimitationAsString))))
            .build();

    verify(detectionRepositoryMock, times(1)).save(expectedDetectionToBeSaved);
  }

  @Test
  void building_api_should_be_called_when_geoJsonDelimitation_type_is_zone() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), ZONE);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    verify(buildingFinderMock, times(1)).retrieveRoofPolygonsFrom(any());
  }

  @Test
  void building_api_should_be_called_when_geoJsonDelimitation_type_is_null() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), null);

    when(detectionRepositoryMock.save(any())).thenReturn(detection);
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    verify(buildingFinderMock, times(1)).retrieveRoofPolygonsFrom(any());
  }

  @Test
  void polygon_is_mapped_to_multipolygon_when_delimitation_type_is_roof() {
    var polygonFeature = polygonFeature();
    var detection = detection(List.of(polygonFeature), ROOF);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    verify(detectionRepositoryMock)
        .save(
            argThat(
                savedDetection -> {
                  var fwd = savedDetection.getFeatureWithDelimitations().getFirst();
                  var delimitations = fwd.delimitations();
                  if (delimitations.size() != 1) {
                    return false;
                  }

                  var delimitation = delimitations.getFirst();
                  return MULTI_POLYGON.equals(delimitation.getGeometry().getGeometryType());
                }));
  }

  @Test
  void provided_geometry_is_used_as_delimitation_when_delimitation_type_is_user_defined() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
    var delimitationCoordinates =
        toRestFeature(delimitation).getGeometry().getMultiPolygon().getCoordinates();
    assertEquals(1, delimitationCoordinates.size());
    assertEquals(1, delimitationCoordinates.getFirst().size());
    assertEquals(PROVIDED_POLYGON_RING, asDoubles(delimitationCoordinates.getFirst().getFirst()));
  }

  @Test
  void building_api_is_called_with_provided_ring_when_delimitation_type_is_user_defined() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<List<BigDecimal>>> ringCaptor = ArgumentCaptor.forClass(List.class);
    verify(buildingFinderMock, times(1)).retrieveRoofPolygonsFrom(ringCaptor.capture());
    assertEquals(PROVIDED_POLYGON_RING, asDoubles(ringCaptor.getValue()));
  }

  @SneakyThrows
  @Test
  void addresses_property_is_computed_from_rnb_when_delimitation_type_is_user_defined() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);
    var addresses = List.of("1 rue de la Paix Paris 75002");
    var serializedAddresses = "[\"1 rue de la Paix Paris 75002\"]";

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(null, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn(serializedAddresses);

    subject.apply(detection);

    assertEquals(serializedAddresses, savedDelimitation().getProperties().get("addresses"));
  }

  @Test
  void addresses_property_is_not_set_when_no_building_is_found_inside_provided_zone() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    assertFalse(savedDelimitation().getProperties().containsKey("addresses"));
  }

  @Test
  void delimitation_is_still_computed_when_building_api_is_unavailable() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenThrow(new BuildingFootPrintSourceUnavailableException("RNB is unavailable"));

    assertDoesNotThrow(() -> subject.apply(detection));

    var delimitation = savedDelimitation();
    assertEquals(List.of(), delimitation.getProperties().get("addresses"));
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
  }

  @Test
  void delimitation_is_still_computed_when_provided_zone_is_larger_than_supported_radius() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenThrow(
            new UnsupportedOperationException(
                "Provided multiPolygon zone is larger than supported retrieving roof polygons"
                    + " radius 1000, actual is 2000"));

    assertDoesNotThrow(() -> subject.apply(detection));

    var delimitation = savedDelimitation();
    assertEquals(List.of(), delimitation.getProperties().get("addresses"));
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
  }

  @Test
  void delimitation_is_still_computed_when_building_api_is_unavailable_for_roof() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), ROOF);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenThrow(new BuildingFootPrintSourceUnavailableException("RNB is unavailable"));

    assertDoesNotThrow(() -> subject.apply(detection));

    var delimitation = savedDelimitation();
    assertEquals(List.of(), delimitation.getProperties().get("addresses"));
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
  }

  @SneakyThrows
  @Test
  void zoom_and_addresses_are_kept_when_provided_polygon_feature_has_no_properties() {
    var providedGeoJson = polygonFeature().toBuilder().properties(null).build();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);
    var addresses = List.of("1 rue de la Paix Paris 75002");
    var serializedAddresses = "[\"1 rue de la Paix Paris 75002\"]";

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(null, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn(serializedAddresses);

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(serializedAddresses, delimitation.getProperties().get("addresses"));
    assertEquals(20, delimitation.getProperties().get("zoom"));
    assertEquals(20, delimitation.getZoom());
  }

  @SneakyThrows
  @Test
  void zoom_and_addresses_are_kept_when_provided_multipolygon_feature_has_no_properties() {
    var providedGeoJson = multiPolygonFeature().toBuilder().properties(null).build();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);
    var addresses = List.of("1 rue de la Paix Paris 75002");
    var serializedAddresses = "[\"1 rue de la Paix Paris 75002\"]";

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(null, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn(serializedAddresses);

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(serializedAddresses, delimitation.getProperties().get("addresses"));
    assertEquals(20, delimitation.getProperties().get("zoom"));
    assertEquals(20, delimitation.getZoom());
  }

  @Test
  void default_zoom_is_applied_when_provided_feature_has_neither_properties_nor_zoom() {
    var providedGeoJson = polygonFeature().toBuilder().properties(null).zoom(null).build();
    var detection = detection(List.of(providedGeoJson), USER_DEFINED_DELIMITATION);

    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(HOUSES_0.getZoomLevel(), delimitation.getProperties().get("zoom"));
    assertEquals(HOUSES_0.getZoomLevel(), delimitation.getZoom());
  }

  private Feature savedDelimitation() {
    var detectionCaptor = ArgumentCaptor.forClass(Detection.class);
    verify(detectionRepositoryMock, times(1)).save(detectionCaptor.capture());

    var featureWithDelimitations = detectionCaptor.getValue().getFeatureWithDelimitations();
    assertEquals(1, featureWithDelimitations.size());
    var delimitations = featureWithDelimitations.getFirst().delimitations();
    assertEquals(1, delimitations.size());
    return delimitations.getFirst();
  }

  private static List<List<Double>> asDoubles(List<List<BigDecimal>> ring) {
    return ring.stream()
        .map(coordinates -> coordinates.stream().map(BigDecimal::doubleValue).toList())
        .toList();
  }

  private static final List<List<Double>> PROVIDED_POLYGON_RING =
      List.of(List.of(0.0, 0.0), List.of(10.0, 0.0), List.of(5.0, 10.0), List.of(0.0, 0.0));

  private static Feature polygonFeature() {
    return Feature.builder()
        .properties(new HashMap<>())
        .zoom(20)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(POLYGON)
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

  private static Feature multiPolygonFeature() {
    return Feature.builder()
        .id(null)
        .zoom(20)
        .properties(new HashMap<>())
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    """
                    {
                      "type": "MultiPolygon",
                      "coordinates": [
                        [
                          [
                            [0.0, 0.0],
                            [10.0, 0.0],
                            [5.0, 10.0],
                            [0.0, 0.0]
                          ]
                        ]
                      ]
                    }
                    """)
                .build())
        .build();
  }

  private static Detection detection(
      List<Feature> providedGeoJson, DelimitationType delimitationType) {
    return Detection.builder()
        .id("detectionId")
        .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
        .providedGeoJsonZone(providedGeoJson)
        .geoJsonDelimitationType(delimitationType)
        .build();
  }

  private static Feature featureWithGeometryAsString(Feature feature) {
    var restFeature = toRestFeature(feature);
    restFeature.putPropertiesItem("zoom", feature.getZoom());
    return toDomainFeature(restFeature);
  }

  private static Feature pointFeature() {
    return Feature.builder()
        .properties(new HashMap<>())
        .zoom(20)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(POINT)
                .actualInstanceStringValue("{\"type\": \"Point\", \"coordinates\": [2.35, 48.85]}")
                .build())
        .build();
  }

  private static Feature twoPolygonsMultiPolygonFeature() {
    return Feature.builder()
        .properties(new HashMap<>())
        .zoom(20)
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    """
                    {
                      "type": "MultiPolygon",
                      "coordinates": [
                        [[[0.0, 0.0], [10.0, 0.0], [5.0, 10.0], [0.0, 0.0]]],
                        [[[20.0, 20.0], [30.0, 20.0], [25.0, 30.0], [20.0, 20.0]]]
                      ]
                    }
                    """)
                .build())
        .build();
  }

  @Test
  void detection_without_roof_based_model_is_returned_untouched_and_not_saved() {
    var detection =
        Detection.builder()
            .id("detectionId")
            .detectableObjectModel(new DetectableObjectModel().modelName(STATIONNEMENT))
            .build();

    var actual = subject.apply(detection);

    assertSame(detection, actual);
    verifyNoInteractions(detectionRepositoryMock);
  }

  @Test
  void parcel_delimitations_are_fetched_from_ign_cadastre_for_parcel_delimitation_type() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), PARCEL);
    var providedGeometry = mock(org.locationtech.jts.geom.Geometry.class);
    var parcelFeature = multiPolygonFeature();
    when(geometryConverterMock.readGeometryFromString(
            providedGeoJson.getGeometry().getActualInstanceStringValue()))
        .thenReturn(providedGeometry);
    when(ignCadastreFeatureFetcher.apply(providedGeometry)).thenReturn(List.of(parcelFeature));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    var detectionCaptor = ArgumentCaptor.forClass(Detection.class);
    verify(detectionRepositoryMock).save(detectionCaptor.capture());
    var parcelDelimitations = detectionCaptor.getValue().getParcelDelimitations();
    assertEquals(1, parcelDelimitations.size());
    assertEquals(providedGeoJson, parcelDelimitations.getFirst().feature());
    assertEquals(List.of(parcelFeature), parcelDelimitations.getFirst().delimitations());
    assertEquals(1, detectionCaptor.getValue().getFeatureWithDelimitations().size());
  }

  @Test
  void roof_delimitations_are_searched_for_a_provided_point_with_its_zoom() {
    var providedGeoJson = pointFeature();
    var detection = detection(List.of(providedGeoJson), ZONE);
    var buildingMultiPolygon = new GeometryFactory().createMultiPolygon();
    var multiPolygonFromPoint = multiPolygonFeature();
    when(buildingFinderMock.getBuildingMultiPolygon(any(Point.class)))
        .thenReturn(buildingMultiPolygon);
    when(geometryConverterMock.toFeature(any(), eq(20), any(), eq(buildingMultiPolygon)))
        .thenReturn(multiPolygonFromPoint);
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    assertEquals(multiPolygonFromPoint, savedDelimitation());
  }

  @Test
  void roof_delimitations_are_searched_for_each_polygon_of_a_provided_multi_polygon() {
    var providedGeoJson = twoPolygonsMultiPolygonFeature();
    var detection = detection(List.of(providedGeoJson), ZONE);
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of());
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    verify(buildingFinderMock, times(2)).retrieveRoofPolygonsFrom(any());
    var detectionCaptor = ArgumentCaptor.forClass(Detection.class);
    verify(detectionRepositoryMock).save(detectionCaptor.capture());
    assertEquals(2, detectionCaptor.getValue().getFeatureWithDelimitations().size());
  }

  @SneakyThrows
  @Test
  void roof_found_inside_provided_polygon_becomes_a_feature_with_its_addresses() {
    var providedGeoJson = polygonFeature();
    var detection = detection(List.of(providedGeoJson), ZONE);
    var addresses = List.of("1 rue de la Paix Paris 75002");
    var roofGeometry = new GeometryFactory().createMultiPolygon();
    var roofFeature = multiPolygonFeature();
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(roofGeometry, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn("serialized addresses");
    when(geometryConverterMock.toFeature(any(), eq(20), any(), eq(roofGeometry)))
        .thenReturn(roofFeature);
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    assertEquals(roofFeature, savedDelimitation());
    var propertiesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(geometryConverterMock)
        .toFeature(
            any(),
            eq(20),
            propertiesCaptor.capture(),
            any(org.locationtech.jts.geom.MultiPolygon.class));
    assertEquals("serialized addresses", propertiesCaptor.getValue().get("addresses"));
  }

  @SneakyThrows
  @Test
  void roof_addresses_serialization_failure_is_reported() {
    var detection = detection(List.of(polygonFeature()), ZONE);
    var roofDetails = new RoofDetails(new GeometryFactory().createMultiPolygon(), List.of("a"));
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any())).thenReturn(List.of(roofDetails));
    when(objectMapperMock.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("serialization failed") {});

    var error = assertThrows(RuntimeException.class, () -> subject.apply(detection));

    assertEquals("serialization failed", error.getCause().getMessage());
  }

  @SneakyThrows
  @Test
  void first_roof_addresses_are_kept_when_several_roofs_are_found_for_a_user_defined_polygon() {
    var detection = detection(List.of(polygonFeature()), USER_DEFINED_DELIMITATION);
    var firstAddresses = List.of("first address");
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(
            List.of(
                new RoofDetails(null, firstAddresses), new RoofDetails(null, List.of("second"))));
    when(objectMapperMock.writeValueAsString(firstAddresses)).thenReturn("first serialized");
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    assertEquals("first serialized", savedDelimitation().getProperties().get("addresses"));
  }

  @Test
  void empty_addresses_are_set_when_rnb_lookup_fails_for_a_user_defined_polygon() {
    var detection = detection(List.of(polygonFeature()), USER_DEFINED_DELIMITATION);
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenThrow(new BuildingFootPrintSourceUnavailableException("rnb unavailable"));
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    assertEquals(List.of(), savedDelimitation().getProperties().get("addresses"));
  }

  @SneakyThrows
  @Test
  void multi_polygon_provided_with_user_defined_delimitation_gets_rnb_addresses() {
    var detection = detection(List.of(multiPolygonFeature()), USER_DEFINED_DELIMITATION);
    var addresses = List.of("2 rue de la Paix Paris 75002");
    when(buildingFinderMock.retrieveRoofPolygonsFrom(any()))
        .thenReturn(List.of(new RoofDetails(null, addresses)));
    when(objectMapperMock.writeValueAsString(addresses)).thenReturn("serialized");
    when(detectionRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection);

    var delimitation = savedDelimitation();
    assertEquals(MULTI_POLYGON, delimitation.getGeometry().getGeometryType());
    assertEquals("serialized", delimitation.getProperties().get("addresses"));
  }

  @Test
  void point_cannot_be_used_as_user_defined_delimitation() {
    var detection = detection(List.of(pointFeature()), USER_DEFINED_DELIMITATION);

    var error = assertThrows(IllegalStateException.class, () -> subject.apply(detection));

    assertEquals("Point is not supported", error.getMessage());
  }
}
