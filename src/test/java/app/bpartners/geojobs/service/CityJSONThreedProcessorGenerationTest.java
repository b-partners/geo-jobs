package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.exception.ThreedApiException;
import app.bpartners.geojobs.service.threed.model.CreateLrgFromFeatureFileUrl;
import app.bpartners.geojobs.service.threed.model.DelimitationType;
import app.bpartners.geojobs.service.threed.model.GenerationStatus;
import app.bpartners.geojobs.service.threed.model.Lrg;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CityJSONThreedProcessorGenerationTest {
  private static final String COMMUNITY_ID = "communityId";
  private static final String API_KEY = "community-api-key";

  BucketComponent bucketComponentMock = mock();
  ThreedApiClient apiClientMock = mock();
  CityJSONThreedProcessor.CityJSONDownloader downloaderMock = mock();
  CommunityAuthorizationRepository communityRepositoryMock = mock();
  CityJSONThreedProcessor subject =
      new CityJSONThreedProcessor(
          new FileWriter(new ObjectMapper(), new ExtensionGuesser()),
          bucketComponentMock,
          apiClientMock,
          downloaderMock,
          communityRepositoryMock);

  @BeforeEach
  void setUp() throws Exception {
    var apiKey = CommunityAuthorizationApiKey.builder().keyValue(API_KEY).build();
    when(communityRepositoryMock.findById(COMMUNITY_ID))
        .thenReturn(
            Optional.of(
                CommunityAuthorization.builder()
                    .id(COMMUNITY_ID)
                    .apiKeys(List.of(apiKey))
                    .build()));
    var presignedUrls = new java.util.concurrent.atomic.AtomicInteger();
    when(bucketComponentMock.presign(anyString(), any()))
        .thenAnswer(
            invocation ->
                new URL("https://bucket.test/building-" + presignedUrls.incrementAndGet()));
    when(downloaderMock.download(anyString()))
        .thenAnswer(invocation -> File.createTempFile("cityjson", ".json"));
  }

  private static Feature polygonFeature() {
    return feature(
        POLYGON, "{\"type\":\"Polygon\",\"coordinates\":[[[2,48],[3,48],[3,49],[2,48]]]}");
  }

  private static Feature multiPolygonFeature() {
    return feature(
        MULTI_POLYGON,
        "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[2,48],[3,48],[3,49],[2,48]]]]}");
  }

  private static Feature pointFeature() {
    return feature(POINT, "{\"type\":\"Point\",\"coordinates\":[2.35,48.85]}");
  }

  private static Feature feature(
      app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum type, String geometryJson) {
    return Feature.builder()
        .properties(new HashMap<>())
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(type)
                .actualInstanceStringValue(geometryJson)
                .build())
        .build();
  }

  private static CityJSONRequest request(
      CityJSONDelimitationObjectType objectType, Feature... features) {
    return CityJSONRequest.builder()
        .id("requestId")
        .communityOwnerId(COMMUNITY_ID)
        .creationDatetime(Instant.now())
        .delimitationObjectType(objectType)
        .delimitations(List.of(features))
        .build();
  }

  private void generationAnswers(Lrg... lrgs) {
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    when(apiClientMock.generate(eq("requestId"), any(), eq(API_KEY)))
        .thenAnswer(invocation -> lrgs[calls.getAndIncrement() % lrgs.length]);
  }

  private static Lrg normalLrg(String fileUrl) {
    return Lrg.builder().status(GenerationStatus.NORMAL).fileUrl(fileUrl).build();
  }

  @Test
  void polygon_is_generated_with_the_community_api_key_and_stored_as_city_json() {
    generationAnswers(normalLrg("https://threed.test/result.json"));

    var actual =
        subject.apply(request(CityJSONDelimitationObjectType.BUILDING_ROOF, polygonFeature()));

    assertEquals(1, actual.size());
    assertEquals("requestId", actual.getFirst().getRequest().getId());
    assertTrue(actual.getFirst().getS3FileKey().endsWith(".json"));
    verify(downloaderMock).download("https://threed.test/result.json");
    verify(bucketComponentMock).upload(any(File.class), eq(actual.getFirst().getS3FileKey()));
    var requestCaptor = ArgumentCaptor.forClass(CreateLrgFromFeatureFileUrl.class);
    verify(apiClientMock).generate(eq("requestId"), requestCaptor.capture(), eq(API_KEY));
    assertEquals("https://bucket.test/building-1", requestCaptor.getValue().getFeatureFileUrl());
    assertEquals(
        DelimitationType.ENTIRE_ROOF_DELIMITATION, requestCaptor.getValue().getDelimitationType());
  }

  @Test
  void roof_segment_face_objects_are_generated_panel_by_panel() {
    generationAnswers(normalLrg("https://threed.test/result.json"));

    subject.apply(
        request(CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE, multiPolygonFeature()));

    var requestCaptor = ArgumentCaptor.forClass(CreateLrgFromFeatureFileUrl.class);
    verify(apiClientMock).generate(eq("requestId"), requestCaptor.capture(), eq(API_KEY));
    assertEquals(
        DelimitationType.PANEL_BY_PANEL_DELIMITATION,
        requestCaptor.getValue().getDelimitationType());
  }

  @Test
  void request_without_delimitation_object_type_uses_the_entire_roof_delimitation() {
    generationAnswers(normalLrg("https://threed.test/result.json"));

    subject.apply(request(null, polygonFeature()));

    var requestCaptor = ArgumentCaptor.forClass(CreateLrgFromFeatureFileUrl.class);
    verify(apiClientMock).generate(eq("requestId"), requestCaptor.capture(), eq(API_KEY));
    assertEquals(
        DelimitationType.ENTIRE_ROOF_DELIMITATION, requestCaptor.getValue().getDelimitationType());
  }

  @Test
  void diagnostics_gather_distinct_warnings_and_messages_of_degraded_buildings_only() {
    generationAnswers(
        normalLrg("https://threed.test/normal.json"),
        Lrg.builder()
            .status(GenerationStatus.DEGRADED)
            .fileUrl("https://threed.test/degraded-1.json")
            .message("1 panel without lidar")
            .warnings(List.of("warning A", "warning B"))
            .build(),
        Lrg.builder()
            .status(GenerationStatus.DEGRADED)
            .fileUrl("https://threed.test/degraded-2.json")
            .message("2 panels without lidar")
            .warnings(List.of("warning B", "warning C"))
            .build());

    var actual =
        subject.applyWithDiagnostics(
            request(
                CityJSONDelimitationObjectType.BUILDING_ROOF,
                polygonFeature(),
                multiPolygonFeature(),
                polygonFeature()));

    assertEquals(3, actual.cityJsons().size());
    assertEquals("1 panel without lidar 2 panels without lidar", actual.degradedMessage());
    assertEquals(List.of("warning A", "warning B", "warning C"), actual.warnings());
  }

  @Test
  void diagnostics_are_empty_when_every_building_is_generated_normally() {
    generationAnswers(normalLrg("https://threed.test/result.json"));

    var actual =
        subject.applyWithDiagnostics(
            request(CityJSONDelimitationObjectType.BUILDING_ROOF, polygonFeature()));

    assertNull(actual.degradedMessage());
    assertTrue(actual.warnings().isEmpty());
  }

  @Test
  void generation_without_lidar_is_reported_and_nothing_is_stored() {
    generationAnswers(
        Lrg.builder().status(GenerationStatus.NO_LIDAR).message("no lidar here").build());
    var request = request(CityJSONDelimitationObjectType.BUILDING_ROOF, polygonFeature());

    var error = assertThrows(ThreedApiException.class, () -> subject.apply(request));

    assertTrue(error.isNoLidarDataFound());
    verify(downloaderMock, never()).download(anyString());
  }

  @Test
  void point_delimitations_are_not_supported() {
    var request = request(CityJSONDelimitationObjectType.BUILDING_ROOF, pointFeature());

    var error = assertThrows(NotImplementedException.class, () -> subject.apply(request));

    assertTrue(error.getMessage().startsWith("Unsupported geometry type for validation"));
  }

  @Test
  void unknown_community_is_rejected() {
    when(communityRepositoryMock.findById(COMMUNITY_ID)).thenReturn(Optional.empty());
    var request = request(CityJSONDelimitationObjectType.BUILDING_ROOF, polygonFeature());

    var error = assertThrows(IllegalArgumentException.class, () -> subject.apply(request));

    assertEquals("No community was found for id = communityId", error.getMessage());
  }

  @Test
  void community_without_api_key_is_rejected() {
    when(communityRepositoryMock.findById(COMMUNITY_ID))
        .thenReturn(Optional.of(CommunityAuthorization.builder().id(COMMUNITY_ID).build()));
    var request = request(CityJSONDelimitationObjectType.BUILDING_ROOF, polygonFeature());

    var error = assertThrows(IllegalArgumentException.class, () -> subject.apply(request));

    assertEquals("No API key was found for community id = communityId", error.getMessage());
  }

  @Test
  void downloader_copies_the_content_of_the_url_into_a_json_file() throws Exception {
    var source = File.createTempFile("source", ".json");
    Files.writeString(source.toPath(), "{\"type\":\"CityJSON\"}");
    var downloader = new CityJSONThreedProcessor.CityJSONDownloader();

    var downloaded = downloader.download(source.toURI().toString());

    assertTrue(downloaded.getName().endsWith(".json"));
    assertEquals("{\"type\":\"CityJSON\"}", Files.readString(downloaded.toPath()));
  }

  @Test
  void downloader_reports_unreadable_urls() {
    var downloader = new CityJSONThreedProcessor.CityJSONDownloader();

    assertThrows(RuntimeException.class, () -> downloader.download("not a valid url"));
  }
}
