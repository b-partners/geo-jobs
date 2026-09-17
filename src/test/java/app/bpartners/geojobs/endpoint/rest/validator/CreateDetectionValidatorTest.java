package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.LOM;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.detection.ModelVersionConf;
import app.bpartners.geojobs.validator.CreateDetectionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Object;

class CreateDetectionValidatorTest {
  private static final String DUMMY_CONF =
      """
{
  "BP_TOITURE": [
    {"objectType": "TOITURE_REVETEMENT", "url": "https://dummy-toiture-v2", "version": "V2"}
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
  CreateDetectionValidator subject =
      new CreateDetectionValidator(new ModelVersionConf(bucketComponentMock, new ObjectMapper()));

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
  void do_not_throws_create_detection_with_mandatory_fields() {
    var createDetectionWithMandatoryFields =
        new CreateDetection()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .detectableObjectModelList(null)
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");
    var createDetectionWithMandatoryFieldsWithEmptyDetectableModelList =
        new CreateDetection()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .detectableObjectModelList(List.of())
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");
    var createDetectionWithMandatoryFieldsUsingDetectableModelList =
        new CreateDetection()
            .detectableObjectModel(null)
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");

    assertDoesNotThrow(() -> subject.accept(createDetectionWithMandatoryFields));
    assertDoesNotThrow(
        () -> subject.accept(createDetectionWithMandatoryFieldsWithEmptyDetectableModelList));
    assertDoesNotThrow(
        () -> subject.accept(createDetectionWithMandatoryFieldsUsingDetectableModelList));
  }

  @Test
  void throws_bad_request_exception_when_mandatory_fields_missing() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(new CreateDetection()));
    var actualWithMissingModelName =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateDetection().detectableObjectModel(new DetectableObjectModel())));

    var expectedMessage =
        "CreateDetection.emailReceiver is mandatory. CreateDetection.zoneName is mandatory. Either"
            + " CreateDetection.detectableObjectModel or CreateDetection.detectableObjectModelList"
            + " is mandatory.";
    assertEquals(expectedMessage, actual.getMessage());
    assertEquals(expectedMessage, actualWithMissingModelName.getMessage());
  }

  @Test
  void do_not_throws_when_defined_model_version_provided() {
    var createDetection =
        new CreateDetection()
            .detectableObjectModelList(
                List.of(new DetectableObjectModel().modelName(TOITURE).modelVersion("1.0.0")))
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");

    assertDoesNotThrow(() -> subject.accept(createDetection));
  }

  @Test
  void throws_bad_request_exception_when_unsupported_model_version_provided() {
    var createDetection =
        new CreateDetection()
            .detectableObjectModelList(
                List.of(new DetectableObjectModel().modelName(TOITURE).modelVersion("9.9.9")))
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");

    var actual = assertThrows(BadRequestException.class, () -> subject.accept(createDetection));
    assertEquals(
        "Model BP_TOITURE has no configuration for version 9.9.9. Available versions: [1.0.0]. ",
        actual.getMessage());
  }

  @Test
  void throws_bad_request_exception_when_model_version_not_defined_for_model() {
    var createDetection =
        new CreateDetection()
            .detectableObjectModelList(
                List.of(new DetectableObjectModel().modelName(LOM).modelVersion("1.0.0")))
            .emailReceiver("email@receiver.com")
            .zoneName("dummy zone name");

    var actual = assertThrows(BadRequestException.class, () -> subject.accept(createDetection));
    assertEquals(
        "Model BP_LOM has no configuration for version 1.0.0. Available versions: []. ",
        actual.getMessage());
  }
}
