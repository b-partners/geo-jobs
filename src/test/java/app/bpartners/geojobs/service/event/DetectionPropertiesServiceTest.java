package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.DelimitationType;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.FeatureRoofResultPropertiesComputer;
import app.bpartners.geojobs.service.area.mutation.MutationContextFactory;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geojson.GeometryCorrector;
import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;

class DetectionPropertiesServiceTest {
  private final MutationContextFactory mutationContextFactoryMock = mock();

  @Test
  void vegetation_is_requested_when_a_vegetation_type_is_among_the_job_configurations() {
    var detection =
        Detection.builder()
            .detectableObjectConfigurations(
                List.of(configurationOf(TOITURE_REVETEMENT), configurationOf(ARBRE)))
            .build();

    assertTrue(DetectionPropertiesService.isVegetationRequested(detection));
  }

  @Test
  void vegetation_is_not_requested_for_a_roof_only_job() {
    var roofOnly =
        Detection.builder()
            .detectableObjectConfigurations(List.of(configurationOf(TOITURE_REVETEMENT)))
            .build();
    var noConfiguration = Detection.builder().build();

    assertFalse(DetectionPropertiesService.isVegetationRequested(roofOnly));
    assertFalse(DetectionPropertiesService.isVegetationRequested(noConfiguration));
  }

  @Test
  void apply_tells_the_computer_whether_the_job_requested_vegetation() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var detectionRepository = mock(DetectionRepository.class);
    var geometryConverter = mock(GeometryConverter.class);
    var tileRepository = mock(MachineDetectedTileRepository.class);
    var subject =
        new DetectionPropertiesService(
            detectionRepository,
            computer,
            geometryConverter,
            tileRepository,
            mock(GeometryCorrector.class),
            mutationContextFactoryMock,
            false);
    var detection =
        Detection.builder()
            .id("detection-id")
            .zdjId("zdj-id")
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .detectableObjectConfigurations(List.of(configurationOf(ARBRE)))
            .build();
    var roof = squareRestFeature();
    when(geometryConverter.apply(any())).thenReturn(squareMultiPolygon());
    when(tileRepository.findAllByZdjJobId("zdj-id")).thenReturn(List.of());
    when(computer.apply(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(new HashMap<>());
    when(detectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.apply(detection, roof, List.of(roof));

    verify(computer).apply(eq(roof), any(), any(), any(), isNull(), eq(true));
  }

  private static Feature squareRestFeature() {
    var ring =
        List.of(
            List.of(BigDecimal.ZERO, BigDecimal.ZERO),
            List.of(BigDecimal.ONE, BigDecimal.ZERO),
            List.of(BigDecimal.ONE, BigDecimal.ONE),
            List.of(BigDecimal.ZERO, BigDecimal.ONE),
            List.of(BigDecimal.ZERO, BigDecimal.ZERO));
    var feature = new Feature();
    feature.setGeometry(
        new FeatureGeometry(
            new MultiPolygon().type(MULTI_POLYGON).coordinates(List.of(List.of(ring)))));
    feature.setProperties(new HashMap<>());
    return feature;
  }

  private static org.locationtech.jts.geom.MultiPolygon squareMultiPolygon() {
    var gf = new GeometryFactory();
    var square =
        gf.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(1, 0),
              new Coordinate(1, 1),
              new Coordinate(0, 1),
              new Coordinate(0, 0)
            });
    return gf.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {square});
  }

  private static DetectableObjectConfiguration configurationOf(DetectableType objectType) {
    return DetectableObjectConfiguration.builder().objectType(objectType).build();
  }

  private DetectionPropertiesService subjectWithMutation(boolean mutationEnabled) {
    return new DetectionPropertiesService(
        mock(DetectionRepository.class),
        mock(FeatureRoofResultPropertiesComputer.class),
        mock(GeometryConverter.class),
        mock(MachineDetectedTileRepository.class),
        mock(GeometryCorrector.class),
        mutationContextFactoryMock,
        mutationEnabled);
  }

  @Test
  void tryCreateMutationContext_returns_the_built_context_on_success_when_mutation_is_enabled() {
    var subject = subjectWithMutation(true);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);
    var expected =
        new MutationContext(
            new AreaPictureHistoryResponse.DatedImage(
                2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg")),
            new AreaPictureHistoryResponse.DatedImage(
                2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg")),
            new File("mask.png"));
    when(mutationContextFactoryMock.create(detection, roofGeometry)).thenReturn(expected);

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertEquals(expected, actual);
  }

  @Test
  void
      tryCreateMutationContext_returns_null_instead_of_propagating_when_factory_throws_and_mutation_is_enabled() {
    var subject = subjectWithMutation(true);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);
    when(mutationContextFactoryMock.create(detection, roofGeometry))
        .thenThrow(new IllegalStateException("geodata API unreachable"));

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertNull(actual);
  }

  @Test
  void tryCreateMutationContext_does_not_call_the_factory_when_mutation_is_disabled() {
    var subject = subjectWithMutation(false);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertNull(actual);
    verifyNoInteractions(mutationContextFactoryMock);
  }

  @SneakyThrows
  private Object invokeTryCreateMutationContext(
      DetectionPropertiesService subject, Detection detection, Geometry roofGeometry) {
    Method method =
        DetectionPropertiesService.class.getDeclaredMethod(
            "tryCreateMutationContext", Detection.class, Geometry.class);
    method.setAccessible(true);
    return method.invoke(subject, detection, roofGeometry);
  }

  private DetectionPropertiesService applySubject(
      FeatureRoofResultPropertiesComputer computer,
      GeometryConverter geometryConverter,
      MachineDetectedTileRepository tileRepository,
      GeometryCorrector geometryCorrector) {
    var detectionRepository = mock(DetectionRepository.class);
    when(detectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(computer.apply(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(new HashMap<>());
    return new DetectionPropertiesService(
        detectionRepository,
        computer,
        geometryConverter,
        tileRepository,
        geometryCorrector,
        mutationContextFactoryMock,
        false);
  }

  private static Detection roofDetection() {
    return Detection.builder()
        .id("detection-id")
        .zdjId("zdj-id")
        .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
        .build();
  }

  private static org.locationtech.jts.geom.Polygon square(
      double minX, double minY, double maxX, double maxY) {
    return new GeometryFactory()
        .createPolygon(
            new Coordinate[] {
              new Coordinate(minX, minY),
              new Coordinate(maxX, minY),
              new Coordinate(maxX, maxY),
              new Coordinate(minX, maxY),
              new Coordinate(minX, minY)
            });
  }

  private static MachineDetectedTile tileWithOneDetectedObject() {
    var ring =
        List.of(
            List.of(BigDecimal.ZERO, BigDecimal.ZERO),
            List.of(BigDecimal.ONE, BigDecimal.ZERO),
            List.of(BigDecimal.ONE, BigDecimal.ONE),
            List.of(BigDecimal.ZERO, BigDecimal.ZERO));
    var objectFeature =
        app.bpartners.geojobs.repository.model.Feature.builder()
            .geometry(
                app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                    .geometryType(
                        app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON)
                    .actualInstanceStringValue(
                        serializeMultiPolygon(
                            new MultiPolygon()
                                .type(MULTI_POLYGON)
                                .coordinates(List.of(List.of(ring)))))
                    .build())
            .build();
    return MachineDetectedTile.builder()
        .tile(Tile.builder().coordinates(new TileCoordinates().x(1).y(1).z(20)).build())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .feature(objectFeature)
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(TOITURE_REVETEMENT).build())
                    .build()))
        .build();
  }

  @SneakyThrows
  private static String serializeMultiPolygon(MultiPolygon multiPolygon) {
    return app.bpartners.geojobs.model.CustomObjectMapper.objectMapper()
        .writeValueAsString(multiPolygon);
  }

  @SuppressWarnings("unchecked")
  private static List<PolygonObjectType> polygonsGivenToComputer(
      FeatureRoofResultPropertiesComputer computer) {
    var captor = ArgumentCaptor.forClass(List.class);
    verify(computer).apply(any(), any(), any(), captor.capture(), any(), anyBoolean());
    return (List<PolygonObjectType>) captor.getValue();
  }

  @Test
  void apply_returns_detection_untouched_when_model_is_not_roof_based() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var detectionRepository = mock(DetectionRepository.class);
    var subject =
        new DetectionPropertiesService(
            detectionRepository,
            computer,
            mock(GeometryConverter.class),
            mock(MachineDetectedTileRepository.class),
            mock(GeometryCorrector.class),
            mutationContextFactoryMock,
            false);
    var detection = Detection.builder().id("detection-id").build();

    var actual = subject.apply(detection, squareRestFeature(), List.of(squareRestFeature()));

    assertEquals(detection, actual);
    verifyNoInteractions(computer);
    verifyNoInteractions(detectionRepository);
  }

  @Test
  void apply_gives_computer_the_detected_object_polygons_inside_the_roof() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var geometryConverter = mock(GeometryConverter.class);
    var tileRepository = mock(MachineDetectedTileRepository.class);
    var geometryCorrector = mock(GeometryCorrector.class);
    var roof = squareRestFeature();
    var subject = applySubject(computer, geometryConverter, tileRepository, geometryCorrector);
    var gf = new GeometryFactory();
    when(geometryConverter.apply(any())).thenReturn(squareMultiPolygon());
    when(tileRepository.findAllByZdjJobId("zdj-id"))
        .thenReturn(List.of(tileWithOneDetectedObject()));
    when(geometryCorrector.apply(any()))
        .thenReturn(
            gf.createMultiPolygon(
                new org.locationtech.jts.geom.Polygon[] {square(0.2, 0.2, 0.5, 0.5)}));

    subject.apply(roofDetection(), roof, List.of(roof));

    var polygons = polygonsGivenToComputer(computer);
    assertEquals(1, polygons.size());
    assertEquals(TOITURE_REVETEMENT, polygons.getFirst().objectType());
    assertEquals(0.09, polygons.getFirst().polygon().getArea(), 1e-9);
  }

  @Test
  void apply_splits_multi_polygon_intersection_into_one_polygon_per_part() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var geometryConverter = mock(GeometryConverter.class);
    var tileRepository = mock(MachineDetectedTileRepository.class);
    var geometryCorrector = mock(GeometryCorrector.class);
    var roof = squareRestFeature();
    var subject = applySubject(computer, geometryConverter, tileRepository, geometryCorrector);
    var gf = new GeometryFactory();
    when(geometryConverter.apply(any())).thenReturn(squareMultiPolygon());
    when(tileRepository.findAllByZdjJobId("zdj-id"))
        .thenReturn(List.of(tileWithOneDetectedObject()));
    when(geometryCorrector.apply(any()))
        .thenReturn(
            gf.createMultiPolygon(
                new org.locationtech.jts.geom.Polygon[] {
                  square(0.0, 0.0, 0.4, 0.4), square(0.6, 0.6, 1.0, 1.0)
                }));

    subject.apply(roofDetection(), roof, List.of(roof));

    assertEquals(2, polygonsGivenToComputer(computer).size());
  }

  @Test
  void apply_ignores_detected_objects_outside_the_roof() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var geometryConverter = mock(GeometryConverter.class);
    var tileRepository = mock(MachineDetectedTileRepository.class);
    var geometryCorrector = mock(GeometryCorrector.class);
    var roof = squareRestFeature();
    var subject = applySubject(computer, geometryConverter, tileRepository, geometryCorrector);
    var gf = new GeometryFactory();
    when(geometryConverter.apply(any())).thenReturn(squareMultiPolygon());
    when(tileRepository.findAllByZdjJobId("zdj-id"))
        .thenReturn(List.of(tileWithOneDetectedObject()));
    when(geometryCorrector.apply(any()))
        .thenReturn(
            gf.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {square(5, 5, 6, 6)}));

    subject.apply(roofDetection(), roof, List.of(roof));

    assertTrue(polygonsGivenToComputer(computer).isEmpty());
  }

  @Test
  void apply_fails_when_feature_does_not_intersect_delimitation_outside_roof_delimitation() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var geometryConverter = mock(GeometryConverter.class);
    var tileRepository = mock(MachineDetectedTileRepository.class);
    var subject =
        applySubject(computer, geometryConverter, tileRepository, mock(GeometryCorrector.class));
    var gf = new GeometryFactory();
    when(geometryConverter.apply(any()))
        .thenReturn(
            squareMultiPolygon(),
            gf.createMultiPolygon(new org.locationtech.jts.geom.Polygon[] {square(5, 5, 6, 6)}));
    var feature = squareRestFeature();
    var detection =
        roofDetection().toBuilder().geoJsonDelimitationType(DelimitationType.PARCEL).build();

    var delimitations = List.of(feature);

    assertThrows(
        IllegalStateException.class, () -> subject.apply(detection, feature, delimitations));
  }

  @Test
  void apply_uses_delimitation_geometry_when_current_feature_is_a_point() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var geometryConverter = mock(GeometryConverter.class);
    var subject =
        applySubject(
            computer,
            geometryConverter,
            mock(MachineDetectedTileRepository.class),
            mock(GeometryCorrector.class));
    when(geometryConverter.apply(any())).thenReturn(squareMultiPolygon());
    var pointFeature = new Feature();
    pointFeature.setGeometry(
        new FeatureGeometry(new Point().coordinates(List.of(BigDecimal.ONE, BigDecimal.ONE))));
    var delimitation = squareRestFeature();

    subject.apply(roofDetection(), pointFeature, List.of(delimitation));

    verify(computer).apply(eq(delimitation), any(), any(), any(), any(), eq(false));
  }

  @Test
  void apply_rejects_unsupported_delimitation_geometry() {
    var computer = mock(FeatureRoofResultPropertiesComputer.class);
    var subject =
        applySubject(
            computer,
            mock(GeometryConverter.class),
            mock(MachineDetectedTileRepository.class),
            mock(GeometryCorrector.class));
    var unsupported = new Feature();
    unsupported.setGeometry(
        new FeatureGeometry(new Point().coordinates(List.of(BigDecimal.ONE, BigDecimal.ONE))));
    var currentFeature = squareRestFeature();
    var detection = roofDetection();
    var delimitations = List.of(unsupported);

    assertThrows(
        IllegalArgumentException.class,
        () -> subject.apply(detection, currentFeature, delimitations));
  }
}
