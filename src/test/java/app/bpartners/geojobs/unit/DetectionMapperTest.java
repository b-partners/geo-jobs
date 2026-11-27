package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.gen.annotator.endpoint.rest.model.Annotation;
import app.bpartners.gen.annotator.endpoint.rest.model.JobStatus;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponseV2;
import app.bpartners.geojobs.service.tiling.TileValidator;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DetectionMapperTest {
  TileValidator tileValidator = new TileValidator();
  DetectionMapper subject = new DetectionMapper(tileValidator);

  DetectionResponseV2 detectionResponse() {
    return DetectionResponseV2.builder().images(Map.of("filename", imageData())).build();
  }

  DetectionResponseV2.ImageData imageData() {
    return DetectionResponseV2.ImageData.builder()
        .base64ImgData("image_base_64")
        .regions(Map.of("regions", region()))
        .build();
  }

  DetectionResponseV2.ImageData.Region region() {
    return DetectionResponseV2.ImageData.Region.builder()
        .regionAttributes(Map.of("label", "PISCINE", "confidence", "0.95"))
        .shapeAttributes(shapes())
        .build();
  }

  DetectionResponseV2.ImageData.ShapeAttributes shapes() {
    return DetectionResponseV2.ImageData.ShapeAttributes.builder()
        .allPointsX(
            List.of(BigDecimal.valueOf(6.958009303660302), BigDecimal.valueOf(43.543013820437459)))
        .allPointsY(
            List.of(BigDecimal.valueOf(6.957965493371299), BigDecimal.valueOf(43.543002082885863)))
        .build();
  }

  @Test
  void map_to_detection_response_to_detected_tile() {
    var parcelId = randomUUID().toString();
    var zoneJobId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var tile =
        Tile.builder()
            .id("tile_id")
            .coordinates(new TileCoordinates().x(5000).y(2000).z(20))
            .build();
    var actual =
        subject.toDetectedTile(detectionResponse(), tile, parcelId, zoneJobId, parcelJobId);

    assertNotNull(actual);
    assertFalse(actual.getDetectedObjects().isEmpty());
    assertEquals(tile, actual.getTile());
    assertEquals(parcelId, actual.getParcelId());
    assertEquals(zoneJobId, actual.getZdjJobId());
    assertEquals(parcelJobId, actual.getParcelJobId());
  }

  @ParameterizedTest
  @CsvSource({
    "roof_ardoise,BATI_ARDOISE",
    "roof_asphalte_bitume,BATI_AUTRES",
    "roof_bac_acier,BATI_AUTRES",
    "roof_beton_brut,BATI_BETON",
    "roof_fibro_ciment,BATI_AUTRES",
    "roof_gravier,BATI_AUTRES",
    "roof_membrane_synthetique,BATI_AUTRES",
    "roof_tole_ondulee,BATI_AUTRES",
    "roof_tuiles,BATI_TUILES",
    "roof_zinc,BATI_AUTRES",
    "zinc,BATI_AUTRES",
    "tole_ondulee,BATI_AUTRES",
    "bac_acier,BATI_AUTRES",
    "asphalte_bitume,BATI_AUTRES",
    "fibre_ciment,BATI_AUTRES",
    "beton_brut,BATI_BETON"
  })
  void map_every_toiture_revetements_and_revets_fp_model_class_to_a_detectable_type(
      String modelClass, DetectableType expected) {
    var tile =
        Tile.builder()
            .id("tile_id")
            .coordinates(new TileCoordinates().x(5000).y(2000).z(20))
            .build();
    var response =
        DetectionResponseV2.builder()
            .images(
                Map.of(
                    "filename",
                    DetectionResponseV2.ImageData.builder()
                        .regions(
                            Map.of(
                                "regions",
                                DetectionResponseV2.ImageData.Region.builder()
                                    .regionAttributes(Map.of("label", modelClass))
                                    .shapeAttributes(shapes())
                                    .build()))
                        .build()))
            .build();

    var actual = subject.toDetectedTile(response, tile, "parcel", "zdj", "parcelJob");

    assertEquals(
        expected,
        actual.getDetectedObjects().getFirst().getDetectedObjectType().getDetectableType());
  }

  @Test
  void ignore_regions_with_unknown_label_instead_of_failing() {
    var tile =
        Tile.builder()
            .id("tile_id")
            .coordinates(new TileCoordinates().x(5000).y(2000).z(20))
            .build();
    var response =
        DetectionResponseV2.builder()
            .images(
                Map.of(
                    "filename",
                    DetectionResponseV2.ImageData.builder()
                        .regions(
                            Map.of(
                                "known",
                                DetectionResponseV2.ImageData.Region.builder()
                                    .regionAttributes(Map.of("label", "PISCINE"))
                                    .shapeAttributes(shapes())
                                    .build(),
                                "unknown",
                                DetectionResponseV2.ImageData.Region.builder()
                                    .regionAttributes(Map.of("label", "not_a_known_label"))
                                    .shapeAttributes(shapes())
                                    .build()))
                        .build()))
            .build();

    var actual = subject.toDetectedTile(response, tile, "parcel", "zdj", "parcelJob");

    assertEquals(1, actual.getDetectedObjects().size());
    assertEquals(
        PISCINE,
        actual.getDetectedObjects().getFirst().getDetectedObjectType().getDetectableType());
  }

  @Test
  void map_annotation_to_human_detected_object() {
    var zoom = 20;
    var tileId = randomUUID().toString();

    var actual = subject.toHumanDetectedObject(zoom, tileId, List.of(annotation()));
    var detectedObject = actual.getFirst();

    assertFalse(actual.isEmpty());
    assertEquals(PISCINE, detectedObject.getDetectableObjectType());
    assertEquals(HUMAN, detectedObject.getType());
    assertEquals(tileId, detectedObject.getDetectedTileId());
    assertEquals(0.9515481, detectedObject.getComputedConfidence());
    Map<String, Object> properties = feature().getProperties();
    properties.put("zoom", zoom);
    properties.put("id", detectedObject.getFeature().getProperties().get("id"));
    assertEquals(feature().properties(properties), detectedObject.getFeature());
  }

  private Annotation annotation() {
    return new Annotation()
        .comment("confidence=95.15481")
        .label(new Label().id(randomUUID().toString()).name("PISCINE").color("#fffff"))
        .polygon(new Polygon().points(List.of(new Point().y(500.0).x(147.5))));
  }

  @SneakyThrows
  private app.bpartners.geojobs.endpoint.rest.model.Feature feature() {
    var coordinates =
        List.of(List.of(List.of(List.of(new BigDecimal("147.5"), new BigDecimal("500.0")))));
    return toRestFeature(
        Feature.builder()
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
            .properties(new HashMap<>())
            .build());
  }

  private static DetectionResponseV2.ImageData.Region regionLabelled(
      String label, String confidence) {
    var attributes = new HashMap<String, String>();
    attributes.put("label", label);
    if (confidence != null) {
      attributes.put("confidence", confidence);
    }
    return DetectionResponseV2.ImageData.Region.builder()
        .regionAttributes(attributes)
        .shapeAttributes(
            DetectionResponseV2.ImageData.ShapeAttributes.builder()
                .allPointsX(List.of(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN))
                .allPointsY(List.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN))
                .build())
        .build();
  }

  @ParameterizedTest
  @CsvSource({
    "ROOF,TOITURE_REVETEMENT",
    "toiture_revetement,TOITURE_REVETEMENT",
    "SOLAR_PANEL,PANNEAU_PHOTOVOLTAIQUE",
    "PV,PANNEAU_PHOTOVOLTAIQUE",
    "PANNEAU_PHOTOVOLTAIQUE,PANNEAU_PHOTOVOLTAIQUE",
    "TREE,ARBRE",
    "ARBRE,ARBRE",
    "PATHWAY,PASSAGE_PIETON",
    "PASSAGE_PIETON,PASSAGE_PIETON",
    "POOL,PISCINE",
    "PISCINE,PISCINE",
    "BATI,BATI",
    "BATI_TUILES,BATI_TUILES",
    "BATI_BETON,BATI_BETON",
    "BATI_ARDOISE,BATI_ARDOISE",
    "BATI_AUTRES,BATI_AUTRES",
    "ZINC,BATI_AUTRES",
    "LINE,LINE",
    "TROTTOIR,TROTTOIR",
    "SIDEWALK,TROTTOIR",
    "PARKING,PARKING",
    "ESPACE_VERT,ESPACE_VERT",
    "GREEN_SPACE,ESPACE_VERT",
    "OBSTACLE,OBSTACLE",
    "CHEMINEE,CHEMINEE",
    "VELUX,VELUX",
    "LUMINEUX,LUMINEUX",
    "USURE_IMPORTANTE_ARDOISE,USURE_IMPORTANTE",
    "USURE_IMPORTANTE_TUILES,USURE_IMPORTANTE",
    "USURE_LEGERE_ARDOISE,USURE_LEGER",
    "USURE_LEGERE_AUTRES,USURE_LEGER",
    "MOISISSURE_COULEUR_ARDOISE,MOISISSURE_COULEUR",
    "MOISISSURE_COULEUR_TUILES,MOISISSURE_COULEUR",
    "MOISISSURE_NOIRCIE_TUILES,MOISISSURE_NOIRCIE",
    "MOISISSURE_CLAIR_TUILES,MOISISSURE_CLAIR",
    "HUMIDITE_CLAIR_AUTRES,HUMIDITE_CLAIR",
    "HUMIDITE_INTENSE_AUTRES,HUMIDITE_INTENSE",
    "PLACE_STANDARD,PLACE_STANDARD"
  })
  void machine_region_label_is_mapped_to_detectable_type(String label, DetectableType expected) {
    var actual = subject.toDetectedObject(regionLabelled(label, "0.9"), "tileId", 20);

    assertEquals(expected, actual.getDetectableObjectType());
    assertEquals("tileId", actual.getDetectedTileId());
    assertEquals(0.9, actual.getComputedConfidence());
  }

  @Test
  void machine_region_with_unknown_or_missing_label_is_ignored() {
    assertNull(subject.toDetectedObject(regionLabelled("UNKNOWN_LABEL", "0.9"), "tileId", 20));
    assertNull(subject.toDetectedObject(regionLabelled(null, "0.9"), "tileId", 20));
  }

  @Test
  void machine_region_confidence_is_optional_and_tolerates_invalid_values() {
    var withoutConfidence = subject.toDetectedObject(regionLabelled("PISCINE", null), "tileId", 20);
    var withInvalidConfidence =
        subject.toDetectedObject(regionLabelled("PISCINE", "not a number"), "tileId", 20);

    assertNull(withoutConfidence.getComputedConfidence());
    assertNull(withInvalidConfidence.getComputedConfidence());
  }

  @Test
  void zone_detection_job_is_created_pending_from_tiling_job() {
    var tilingJob =
        ZoneTilingJob.builder()
            .id("tilingJobId")
            .zoneName("zoneName")
            .emailReceiver("receiver@example.com")
            .build();

    var actual = subject.fromTilingJob(tilingJob);

    assertEquals("zoneName", actual.getZoneName());
    assertEquals("receiver@example.com", actual.getEmailReceiver());
    assertEquals(tilingJob, actual.getZoneTilingJob());
    assertEquals(ZoneDetectionJob.DetectionType.MACHINE, actual.getDetectionType());
    assertEquals(Status.ProgressionStatus.PENDING, actual.getStatus().getProgression());
    assertEquals(Status.HealthStatus.UNKNOWN, actual.getStatus().getHealth());
  }

  @ParameterizedTest
  @CsvSource({
    "COMPLETED,FINISHED,SUCCEEDED",
    "FAILED,FINISHED,FAILED",
    "STARTED,PROCESSING,UNKNOWN",
    "PENDING,PENDING,UNKNOWN",
    "READY,PENDING,UNKNOWN",
    "TO_REVIEW,PENDING,UNKNOWN",
    "TO_CORRECT,PENDING,UNKNOWN"
  })
  void annotation_job_status_is_mapped_to_progression_and_health(
      String annotationStatus, Status.ProgressionStatus progression, Status.HealthStatus health) {
    var annotationJobStatus = JobStatus.fromValue(annotationStatus);

    assertEquals(progression, subject.getProgressionStatus(annotationJobStatus));
    assertEquals(health, subject.getHealthStatus(annotationJobStatus));
  }

  @Test
  void missing_annotation_job_status_gives_no_progression_nor_health() {
    assertNull(subject.getProgressionStatus(null));
    assertNull(subject.getHealthStatus(null));
  }

  private static Annotation annotation(String labelName, String comment, boolean withPolygon) {
    var annotation = new Annotation().label(new Label().name(labelName)).comment(comment);
    if (withPolygon) {
      annotation.polygon(
          new Polygon()
              .points(
                  List.of(
                      new Point().x(1.0).y(2.0),
                      new Point().x(3.0).y(4.0),
                      new Point().x(null).y(5.0))));
    }
    return annotation;
  }

  @ParameterizedTest
  @CsvSource({
    "TOITURE_REVETEMENT",
    "PANNEAU_PHOTOVOLTAIQUE",
    "ARBRE",
    "PASSAGE_PIETON",
    "PISCINE",
    "BATI_TUILES",
    "BATI_BETON",
    "BATI_ARDOISE",
    "BATI_AUTRES",
    "LINE",
    "TROTTOIR",
    "PARKING",
    "PLACE_STANDARD",
    "ESPACE_VERT"
  })
  void human_annotation_label_is_mapped_to_detectable_type(DetectableType expected) {
    var annotations = List.of(annotation(expected.name().toLowerCase(), "confidence=80", true));

    var actual = subject.toHumanDetectedObject(20, "tileId", annotations);

    assertEquals(1, actual.size());
    assertEquals(expected, actual.getFirst().getDetectableObjectType());
    assertEquals(0.8, actual.getFirst().getComputedConfidence(), 0.0001);
    assertEquals("tileId", actual.getFirst().getDetectedTileId());
  }

  @Test
  void human_annotation_without_comment_has_no_confidence() {
    var actual =
        subject.toHumanDetectedObject(20, "tileId", List.of(annotation("PISCINE", null, true)));

    assertNull(actual.getFirst().getComputedConfidence());
  }

  @Test
  void human_annotation_without_polygon_gives_no_detected_object() {
    var actual =
        subject.toHumanDetectedObject(20, "tileId", List.of(annotation("PISCINE", null, false)));

    assertEquals(1, actual.size());
    assertNull(actual.getFirst());
  }

  @Test
  void human_annotation_with_unknown_label_is_rejected() {
    var annotations = List.of(annotation("UNKNOWN", null, true));

    var error =
        assertThrows(
            IllegalStateException.class,
            () -> subject.toHumanDetectedObject(20, "tileId", annotations));

    assertEquals("Unexpected value: UNKNOWN", error.getMessage());
  }

  @Test
  void human_annotation_without_label_name_is_rejected() {
    var annotations = List.of(annotation(null, null, true));

    assertThrows(
        IllegalArgumentException.class,
        () -> subject.toHumanDetectedObject(20, "tileId", annotations));
  }

  @Test
  void human_annotation_without_polygon_points_has_no_feature() {
    var annotation = new Annotation().label(new Label().name("PISCINE")).polygon(new Polygon());

    var actual = subject.toHumanDetectedObject(20, "tileId", List.of(annotation));

    assertNull(actual.getFirst().getFeature());
  }
}
