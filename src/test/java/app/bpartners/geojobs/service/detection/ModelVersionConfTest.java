package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.LOM;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.VEGETATION;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PLACE_STANDARD;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.service.detection.DetectionApiVersion.V1;
import static app.bpartners.geojobs.service.detection.DetectionApiVersion.V2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Object;

class ModelVersionConfTest {
  private static final String DUMMY_CONF =
      """
{
  "BP_TOITURE": [
    {"objectType": "TOITURE_REVETEMENT", "url": "https://dummy-toiture-v2", "version": "V2"},
    {"objectType": "VELUX", "url": "https://dummy-toiture-v2", "version": "V2"}
  ],
  "BP_VEGETATION": [],
  "BP_STATIONNEMENT": [
    {"objectType": "PLACE_STANDARD", "url": "https://dummy-stationnement-v1", "version": "V1"}
  ]
}""";

  private static final String BUCKET_NAME = "dummyBucket";
  private static final String CONF_PREFIX = "conf/null/model-versions/";

  CustomBucketComponent bucketComponentMock = mock(CustomBucketComponent.class);
  BucketConf bucketConfMock = mock(BucketConf.class);
  ModelVersionConf subject = new ModelVersionConf(bucketComponentMock, new ObjectMapper());

  @BeforeEach
  @SneakyThrows
  void setUp() {
    var confFile = Files.createTempFile("model-version", ".json");
    Files.writeString(confFile, DUMMY_CONF);
    when(bucketComponentMock.getBucketConf()).thenReturn(bucketConfMock);
    when(bucketConfMock.getBucketName()).thenReturn(BUCKET_NAME);
    when(bucketComponentMock.listObjects(BUCKET_NAME, CONF_PREFIX))
        .thenReturn(List.of(S3Object.builder().key(CONF_PREFIX + "1.0.0.json").build()));
    when(bucketComponentMock.download(BUCKET_NAME, CONF_PREFIX + "1.0.0.json"))
        .thenReturn(confFile.toFile());
  }

  @Test
  void resolves_earliest_version_when_none_provided() {
    assertEquals("1.0.0", subject.resolveVersion(null));
    assertEquals("1.0.0", subject.resolveVersion(""));
    assertEquals("1.0.0", subject.resolveVersion("1.0.0"));
  }

  @Test
  void throws_when_unsupported_version_resolved() {
    var actual = assertThrows(BadRequestException.class, () -> subject.resolveVersion("2.0.0"));
    assertEquals("Unsupported model version 2.0.0", actual.getMessage());
  }

  @Test
  void loads_toiture_urls_from_s3_conf() {
    var urls = subject.getTileDetectorUrls(TOITURE, null);

    assertEquals(2, urls.size());
    var toitureRevetement =
        urls.stream()
            .filter(u -> u.getObjectType() == TOITURE_REVETEMENT)
            .findFirst()
            .orElseThrow();
    assertEquals("https://dummy-toiture-v2", toitureRevetement.getUrl());
    assertEquals(V2, toitureRevetement.getVersion());
  }

  @Test
  void loads_stationnement_urls_from_s3_conf() {
    var urls = subject.getTileDetectorUrls(STATIONNEMENT, "1.0.0");

    assertEquals(1, urls.size());
    var placeStandard =
        urls.stream().filter(u -> u.getObjectType() == PLACE_STANDARD).findFirst().orElseThrow();
    assertEquals("https://dummy-stationnement-v1", placeStandard.getUrl());
    assertEquals(V1, placeStandard.getVersion());
  }

  @Test
  void vegetation_is_defined_and_relies_on_default_endpoint() {
    assertTrue(subject.hasConfiguration(VEGETATION, "1.0.0"));
    assertTrue(subject.getTileDetectorUrls(VEGETATION, "1.0.0").isEmpty());
  }

  @Test
  void has_no_configuration_for_undefined_model() {
    assertTrue(subject.hasConfiguration(LOM, null));
    assertFalse(subject.hasConfiguration(LOM, "1.0.0"));
  }

  @Test
  void exposes_available_versions_per_model() {
    assertEquals(Set.of("1.0.0"), subject.getAvailableVersions(TOITURE));
    assertEquals(Set.of("1.0.0"), subject.getAvailableVersions(VEGETATION));
    assertEquals(Set.of(), subject.getAvailableVersions(LOM));
  }
}
