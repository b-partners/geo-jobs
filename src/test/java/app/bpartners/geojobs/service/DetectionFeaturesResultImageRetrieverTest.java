package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.validator.FeatureTypeChecker;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.S3Object;

class DetectionFeaturesResultImageRetrieverTest {
  private static final String PRE_SIGNED_S3_URL = "http://presigned-s3-url.com";
  FeatureTypeChecker featureTypeChecker = new FeatureTypeChecker();
  BucketComponent bucketComponentMock = mock();
  CustomBucketComponent customBucketComponentMock = mock();
  DetectionFeaturesResultImageRetriever subject =
      new DetectionFeaturesResultImageRetriever(
          bucketComponentMock, featureTypeChecker, customBucketComponentMock);

  @BeforeEach
  void setUp() {
    when(customBucketComponentMock.listObjects(any(), any()))
        .thenReturn(List.of(mock(S3Object.class))) // Get only original image
        .thenReturn(List.of()); // Do not retrieve drawn image
  }

  @SneakyThrows
  @Test
  void retrieve_feature_extended_original_image_from_bucket() {
    var detectionMock = mock(Detection.class);
    var latitude = BigDecimal.valueOf(46.651930);
    var longitude = BigDecimal.valueOf(-0.249317);
    var layer = "cite:PCRS";
    var features = List.of(somePoint(longitude, latitude, null));
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(features);
    when(detectionMock.getDetectableObjectModel())
        .thenReturn(new DetectableObjectModel().modelName(TOITURE));
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layer)));
    when(bucketComponentMock.presign(
            layer + "/extended_original_" + longitude + "_" + latitude + ".jpg",
            Duration.ofHours(1L)))
        .thenReturn(new URI(PRE_SIGNED_S3_URL).toURL());

    var actual = subject.apply(detectionMock);

    var expectedFeatures = expectedFeaturesContainingImageUrl(longitude, latitude);
    assertEquals(expectedFeatures.toString(), actual.toString());
  }

  @SneakyThrows
  @Test
  void return_provided_geo_json_when_layer_null() {
    var detectionMock = mock(Detection.class);
    var latitude = BigDecimal.valueOf(46.651930);
    var longitude = BigDecimal.valueOf(-0.249317);
    var features = List.of(somePoint(longitude, latitude, null));
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(features);
    when(detectionMock.getDetectableObjectModel())
        .thenReturn(new DetectableObjectModel().modelName(TOITURE));
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(null)));

    var actual = subject.apply(detectionMock);

    assertEquals(features.toString(), actual.toString());
  }

  @SneakyThrows
  @Test
  void return_provided_geo_json_when_disable_to_presign_bucket() {
    var detectionMock = mock(Detection.class);
    var latitude = BigDecimal.valueOf(46.651930);
    var longitude = BigDecimal.valueOf(-0.249317);
    var layer = "cite:PCRS";
    var features = List.of(somePoint(longitude, latitude, null));
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(features);
    when(detectionMock.getDetectableObjectModel())
        .thenReturn(new DetectableObjectModel().modelName(TOITURE));
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layer)));
    when(bucketComponentMock.presign(
            layer + "/extended_original_" + longitude + "_" + latitude + ".jpg",
            Duration.ofHours(1L)))
        .thenThrow(AwsServiceException.class);

    var actual = subject.apply(detectionMock);

    assertEquals(features.toString(), actual.toString());
  }

  @Test
  void return_null_when_provided_geo_json_is_null() {
    var detectionMock = mock(Detection.class);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(null);

    var actual = subject.apply(detectionMock);

    assertNull(actual);
  }

  @SneakyThrows
  @Test
  void skip_pre_sign_key_when_bucket_exists() {
    reset(customBucketComponentMock);
    var detectionMock = mock(Detection.class);
    var latitude = BigDecimal.valueOf(46.651930);
    var longitude = BigDecimal.valueOf(-0.249317);
    var layer = "cite:PCRS";
    var features = List.of(somePoint(longitude, latitude, null));
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(features);
    when(detectionMock.getDetectableObjectModel())
        .thenReturn(new DetectableObjectModel().modelName(TOITURE));
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layer)));
    when(customBucketComponentMock.listObjects(any(), any())).thenReturn(List.of());

    var actual = subject.apply(detectionMock);

    assertEquals(features.toString(), actual.toString());
    verify(bucketComponentMock, never()).presign(any(), any());
  }

  private List<Feature> expectedFeaturesContainingImageUrl(
      BigDecimal longitude, BigDecimal latitude) {
    var properties = new HashMap<String, Object>();
    properties.put("original_image_url", PRE_SIGNED_S3_URL);
    return List.of(somePoint(longitude, latitude, properties));
  }

  private static Feature somePoint(
      BigDecimal longitude, BigDecimal latitude, HashMap<String, Object> properties) {
    return new Feature()
        .type(FEATURE)
        .geometry(new FeatureGeometry(new Point().coordinates(List.of(longitude, latitude))))
        .properties(properties);
  }
}
