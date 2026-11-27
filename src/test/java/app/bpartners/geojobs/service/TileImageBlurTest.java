package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.ZONE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.DelimitationType;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.utils.ImageComparator;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

class TileImageBlurTest {
  GeometryConverter geometryConverter = new GeometryConverter();
  DetectionZoneToProcessProvider providedZoneUnifierMock = mock();
  DetectionBackgroundRetriever detectionBackgroundRetrieverMock =
      new DetectionBackgroundRetriever(providedZoneUnifierMock, geometryConverter);
  GeometryPixelProjector geometryPixelProjector = new GeometryPixelProjector();
  TileImageBlur subject =
      new TileImageBlur(
          geometryPixelProjector,
          geometryConverter,
          new FilePolygonDrawer(),
          detectionBackgroundRetrieverMock,
          providedZoneUnifierMock);
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  ImageComparator imageComparator = new ImageComparator();

  @SneakyThrows
  @Test
  void return_blured_image() {
    var detectionMock = mock(Detection.class);
    var detectionId = randomUUID().toString();

    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.hasRoofBasedModelName()).thenReturn(true);
    when(detectionMock.getGeoJsonDelimitationType()).thenReturn(ZONE);
    when(detectionMock.getDetectableObjectModel())
        .thenReturn(new DetectableObjectModel().modelName(TOITURE));
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(List.of(featureWithDelimitation()));
    when(providedZoneUnifierMock.apply(detectionMock)).thenReturn(providedMultiPolygon());

    var actual =
        subject.apply(
            detectionMock,
            List.of(
                Tile.builder()
                    .image(
                        new ClassPathResource(
                                "images/ALPES-MARITIMES_2024_5cm/20/544681/383095.jpg")
                            .getFile())
                    .coordinates(new TileCoordinates().x(544681).y(383095).z(20))
                    .build(),
                Tile.builder()
                    .image(
                        new ClassPathResource(
                                "images/ALPES-MARITIMES_2024_5cm/20/544680/383095.jpg")
                            .getFile())
                    .coordinates(new TileCoordinates().x(544680).y(383095).z(20))
                    .build()));

    var expectedOne =
        new ClassPathResource("images/ALPES-MARITIMES_2024_5cm/expected/1.jpg").getFile();
    var expectedTwo =
        new ClassPathResource("images/ALPES-MARITIMES_2024_5cm/expected/2.jpg").getFile();
    var actualSortedByX =
        actual.stream().sorted(Comparator.comparing(tile -> tile.getCoordinates().getX())).toList();
    assertTrue(imageComparator.apply(actualSortedByX.getFirst().getImage(), expectedOne));
    assertTrue(imageComparator.apply(actualSortedByX.getLast().getImage(), expectedTwo));
  }

  private MultiPolygon latLonBackgroundInsideProvidedZone() {
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(7.001416587400087, 43.55065486194394),
          new Coordinate(7.001289026730461, 43.55064682267633),
          new Coordinate(7.0012428090958565, 43.550616005472136),
          new Coordinate(7.001240960390533, 43.55056241029783),
          new Coordinate(7.001416587400087, 43.55065486194394)
        };
    LinearRing shell = geometryFactory.createLinearRing(coords);
    Polygon polygon = geometryFactory.createPolygon(shell);
    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
  }

  private MultiPolygon providedMultiPolygon() {
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(7.0012391116852655, 43.55070041777557),
          new Coordinate(7.001155919943699, 43.550644142919765),
          new Coordinate(7.001150373826647, 43.55056107041793),
          new Coordinate(7.0012243220417645, 43.55055705077757),
          new Coordinate(7.0012391116852655, 43.55061064595711),
          new Coordinate(7.001289026730461, 43.550644142919765),
          new Coordinate(7.0012391116852655, 43.55070041777557)
        };
    LinearRing shell = geometryFactory.createLinearRing(coords);
    Polygon polygon = geometryFactory.createPolygon(shell);
    return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
  }

  @SneakyThrows
  private Feature providedFeature() {
    var featureStringValue =
        """
        {
              "type": "Feature",
              "properties": {},
              "geometry": {
                "coordinates": [
                  [
                    [
                      7.0012391116852655,
                      43.55070041777557
                    ],
                    [
                      7.001155919943699,
                      43.550644142919765
                    ],
                    [
                      7.001150373826647,
                      43.55056107041793
                    ],
                    [
                      7.0012243220417645,
                      43.55055705077757
                    ],
                    [
                      7.0012391116852655,
                      43.55061064595711
                    ],
                    [
                      7.001289026730461,
                      43.550644142919765
                    ],
                    [
                      7.0012391116852655,
                      43.55070041777557
                    ]
                  ]
                ],
                "type": "Polygon"
              }
            }""";
    return objectMapper.readValue(featureStringValue, Feature.class);
  }

  @SneakyThrows
  private FeatureWithDelimitation featureWithDelimitation() {
    var domainStringValue =
        """
{"feature": {"id": null, "zoom": null, "geometry": {"geometryType": "Polygon", "actualInstanceStringValue": "{\\"coordinates\\":[[[7.0012391116852655,43.55070041777557],[7.001155919943699,43.550644142919765],[7.001150373826647,43.55056107041793],[7.0012243220417645,43.55055705077757],[7.0012391116852655,43.55061064595711],[7.001289026730461,43.550644142919765],[7.0012391116852655,43.55070041777557]]],\\"type\\":\\"Polygon\\"}"}, "properties": {}}, "delimitations": [{"id": "26d3a878-2338-46b9-b6c9-cf6c3cbd900a", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.001409385467734,43.55071182764997],[7.001150299949268,43.55070877227807],[7.001143004582222,43.55063967290642],[7.00116400714788,43.55063889819283],[7.001158197839128,43.55057334756624],[7.001233497239312,43.550569669104384],[7.001235484564402,43.55058040646799],[7.00124907444506,43.55057990517163],[7.00125133009923,43.55061225397956],[7.001234033877726,43.55061289199318],[7.001235036387186,43.550627269241545],[7.001241150953786,43.55062614280176],[7.001269834513988,43.550646706078226],[7.001271383246328,43.550651153395584],[7.001406360059308,43.55065067877436],[7.001409385467734,43.55071182764997]]]]}"}, "properties": {}}]}""";

    return objectMapper.readValue(domainStringValue, FeatureWithDelimitation.class);
  }

  DetectionBackgroundRetriever backgroundRetrieverMock = mock();
  TileImageBlur subjectWithMockedBackground =
      new TileImageBlur(
          geometryPixelProjector,
          geometryConverter,
          new FilePolygonDrawer(),
          backgroundRetrieverMock,
          providedZoneUnifierMock);

  private static final int TILE_X = 544681;
  private static final int TILE_Y = 383095;

  @SneakyThrows
  private Tile tileAt(int x, int y) {
    return Tile.builder()
        .image(
            new ClassPathResource("images/ALPES-MARITIMES_2024_5cm/20/544681/383095.jpg").getFile())
        .coordinates(new TileCoordinates().x(x).y(y).z(20))
        .build();
  }

  private Detection detectionMock(DelimitationType delimitationType) {
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(randomUUID().toString());
    when(detectionMock.hasRoofBasedModelName()).thenReturn(true);
    when(detectionMock.getGeoJsonDelimitationType()).thenReturn(delimitationType);
    when(detectionMock.getDetectableObjectModel())
        .thenReturn(new DetectableObjectModel().modelName(TOITURE));
    when(providedZoneUnifierMock.apply(detectionMock)).thenReturn(providedMultiPolygon());
    return detectionMock;
  }

  @SneakyThrows
  private boolean isMostlyWhiteAtTileCenter(Tile tile) {
    BufferedImage image = ImageIO.read(tile.getImage());
    var rgb = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
    return rgb.getRed() > 200 && rgb.getGreen() > 200 && rgb.getBlue() > 200;
  }

  @Test
  void blur_tile_outside_the_polygon_whitens_the_whole_image() {
    var outsideTile = tileAt(TILE_X + 20, TILE_Y);

    var actual = subject.apply(providedMultiPolygon(), List.of(outsideTile));

    assertEquals(1, actual.size());
    assertEquals(outsideTile.getCoordinates(), actual.getFirst().getCoordinates());
    assertTrue(isMostlyWhiteAtTileCenter(actual.getFirst()));
  }

  @Test
  void blur_tile_crossing_the_polygon_keeps_the_polygon_visible() {
    var crossingTile = tileAt(TILE_X, TILE_Y);

    var actual = subject.apply(providedMultiPolygon(), List.of(crossingTile));

    assertEquals(1, actual.size());
    assertNotNull(actual.getFirst().getImage());
    assertNotEquals(crossingTile.getImage(), actual.getFirst().getImage());
  }

  @SneakyThrows
  @Test
  void blur_tile_with_unreadable_image_has_no_image() {
    var notAnImage = File.createTempFile("not-an-image", ".jpg");
    Files.writeString(notAnImage.toPath(), "not an image");
    var tile = tileAt(TILE_X, TILE_Y).toBuilder().image(notAnImage).build();

    var actual = subject.apply(providedMultiPolygon(), List.of(tile));

    assertNull(actual.getFirst().getImage());
  }

  @SneakyThrows
  @Test
  void blur_outside_roof_delimitation_keeps_the_provided_zone_visible() {
    var detectionMock = detectionMock(ROOF);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(List.of(featureWithDelimitation()));

    var actual = subject.apply(detectionMock, List.of(tileAt(TILE_X, TILE_Y)));

    assertEquals(1, actual.size());
    assertNotNull(actual.getFirst().getImage());
    assertFalse(actual.getFirst().isBlured());
  }

  @SneakyThrows
  @Test
  void blur_outside_parcel_delimitations_uses_the_unified_parcels() {
    var detectionMock = detectionMock(PARCEL);
    when(detectionMock.getParcelDelimitations()).thenReturn(List.of(featureWithDelimitation()));
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(List.of(featureWithDelimitation()));
    var tiles = List.of(tileAt(TILE_X, TILE_Y), tileAt(TILE_X - 1, TILE_Y));

    var actual = subject.apply(detectionMock, tiles);

    assertEquals(2, actual.size());
    assertTrue(actual.stream().anyMatch(tile -> !tile.isBlured()));
    assertTrue(actual.stream().map(Tile::getImage).allMatch(Objects::nonNull));
  }

  @SneakyThrows
  @Test
  void original_tiles_are_returned_when_every_tile_ends_up_blurred() {
    var detectionMock = detectionMock(ZONE);
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(List.of(featureWithDelimitation()));
    var tiles = List.of(tileAt(TILE_X + 30, TILE_Y), tileAt(TILE_X + 31, TILE_Y));

    var actual = subject.apply(detectionMock, tiles);

    assertSame(tiles, actual);
  }

  @SneakyThrows
  @Test
  void unify_delimitations_fails_when_delimitations_are_empty() {
    when(backgroundRetrieverMock.apply(any())).thenReturn(providedMultiPolygon());
    var detectionMock = detectionMock(ZONE);
    when(detectionMock.getFeatureWithDelimitations()).thenReturn(List.of());
    var tiles = List.of(tileAt(TILE_X, TILE_Y));

    var error =
        assertThrows(
            IllegalStateException.class,
            () -> subjectWithMockedBackground.apply(detectionMock, tiles));

    assertTrue(error.getMessage().startsWith("Unable to unify delimitation multiPolygon"));
  }

  @SneakyThrows
  @Test
  void unify_parcels_fails_when_there_are_no_parcel_delimitations() {
    when(backgroundRetrieverMock.apply(any())).thenReturn(providedMultiPolygon());
    var detectionMock = detectionMock(PARCEL);
    when(detectionMock.getParcelDelimitations()).thenReturn(List.of());
    var tiles = List.of(tileAt(TILE_X, TILE_Y));

    var error =
        assertThrows(
            IllegalStateException.class,
            () -> subjectWithMockedBackground.apply(detectionMock, tiles));

    assertTrue(error.getMessage().startsWith("Unable to unify delimitation multiPolygon"));
  }

  @SneakyThrows
  @Test
  void delimitation_geometry_other_than_polygon_is_rejected() {
    when(backgroundRetrieverMock.apply(any())).thenReturn(providedMultiPolygon());
    var detectionMock = detectionMock(ZONE);
    var pointDelimitation =
        app.bpartners.geojobs.repository.model.Feature.builder()
            .properties(new java.util.HashMap<>())
            .geometry(
                app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                    .geometryType(app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT)
                    .actualInstanceStringValue("{\"type\":\"Point\",\"coordinates\":[7.001,43.55]}")
                    .build())
            .build();
    when(detectionMock.getFeatureWithDelimitations())
        .thenReturn(
            List.of(new FeatureWithDelimitation(pointDelimitation, List.of(pointDelimitation))));
    var tiles = List.of(tileAt(TILE_X, TILE_Y));

    assertThrows(
        IllegalArgumentException.class,
        () -> subjectWithMockedBackground.apply(detectionMock, tiles));
  }
}
