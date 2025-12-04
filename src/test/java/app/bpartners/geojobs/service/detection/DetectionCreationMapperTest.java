package app.bpartners.geojobs.service.detection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.GeoJsonDelimitationTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureTypeChecker;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.geoserver.GeoServerConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DetectionCreationMapperTest {

  DetectableObjectTypeMapper detectableObjectTypeMapper = new DetectableObjectTypeMapper();
  FeatureTypeChecker featureTypeChecker = new FeatureTypeChecker();
  GeoJsonDelimitationTypeMapper geoJsonDelimitationTypeMapper = new GeoJsonDelimitationTypeMapper();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  AreaPictureApi areaPictureApiMock = mock();
  GeoServerConfiguration geoServerConfigurationMock = mock();

  DetectionCreationMapper subject =
      new DetectionCreationMapper(
          detectableObjectTypeMapper,
          featureTypeChecker,
          communityAuthorizationRepositoryMock,
          areaPictureApiMock,
          geoServerConfigurationMock,
          geoJsonDelimitationTypeMapper);

  @Test
  void map_detection_with_detectable_object_model_list_ok() {
    var detectionE2Id = UUID.randomUUID().toString();
    var communityOwnerId = UUID.randomUUID().toString();
    var createDetection = createDetection();

    var actual = subject.apply(createDetection, detectionE2Id, communityOwnerId, false);

    assertFalse(actual.getDetectableObjectModelList().isEmpty());
    assertNull(actual.getDetectableObjectModel());
    assertFalse(actual.getDetectableObjectConfigurations().isEmpty());
  }

  @Test
  void map_detection_with_no_detectable_object_model_list_ok() {
    var detectionE2Id = UUID.randomUUID().toString();
    var communityOwnerId = UUID.randomUUID().toString();
    var createDetection = createDetection().detectableObjectModelList(null);

    var actual = subject.apply(createDetection, detectionE2Id, communityOwnerId, false);

    assertNotNull(actual.getDetectableObjectModel());
    assertFalse(actual.getDetectableObjectConfigurations().isEmpty());
  }

  private CreateDetection createDetection() {
    return new CreateDetection()
        .detectableObjectModel(detectableObjectModel())
        .detectableObjectModelList(detectableObjectModelList())
        .emailReceiver("<EMAIL>");
  }

  private List<DetectableObjectModel> detectableObjectModelList() {
    return List.of(
        new DetectableObjectModel().modelName(ModelName.TOITURE),
        new DetectableObjectModel().modelName(ModelName.VEGETATION));
  }

  private DetectableObjectModel detectableObjectModel() {
    return new DetectableObjectModel().modelName(ModelName.TOITURE);
  }
}
