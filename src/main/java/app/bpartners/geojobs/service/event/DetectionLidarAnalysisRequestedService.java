package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.EXTRACTION_ERROR;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.PENDING;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.model.DetectionLidarAnalysisRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.DetectionCityJSONGenerator;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor.RoofsAnalysisResult;
import app.bpartners.geojobs.service.lidar.model.LidarDataStatus;
import app.bpartners.geojobs.service.lidar.model.geometry.planes.Plane3D;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionLidarAnalysisRequestedService
    implements Consumer<DetectionLidarAnalysisRequested> {
  public static final String ROOF_SLOPE_PROPERTY_NAME = "roof_slope_in_degrees";
  public static final String ROOF_HEIGHT_PROPERTY_NAME = "roof_height_in_meters";
  public static final String LIDAR_DATA_STATUS_PROPERTY_NAME = "lidar_data_status";

  private final FeatureMapper featureMapper;
  private final EntityManager entityManager;
  private final DetectionRepository detectionRepository;
  private final FeatureVggRequestedService zoneVggRequestedService;
  private final LidarRoofsAnalysisProcessor lidarRoofsAnalysisProcessor;
  private final DetectionCityJSONGenerator detectionCityJSONGenerator;
  private final Workers workers;

  @Override
  public void accept(DetectionLidarAnalysisRequested requested) {
    var detectionIdentifier = requested.getDetectionId();
    var detection =
        detectionRepository
            .findById(detectionIdentifier)
            .orElseThrow(
                () -> new RuntimeException("Detection={" + detectionIdentifier + "} not found"));

    var featureWithDelimitations = detection.getFeatureWithDelimitations();
    if (featureWithDelimitations == null) {
      log.error("FeatureWithDelimitation is null for detection={{}}", detectionIdentifier);
      return;
    }

    if (detection.isLidarAnalysisAlreadyProcessedAsSuccess()) {
      log.warn("Detection={{}} lidar properties has already been processed", detectionIdentifier);
      return;
    }

    try {
      updateLidarDataStatus(detection, PENDING);
      compute(detectionIdentifier, featureWithDelimitations);
    } catch (Exception e) {
      log.error(e.getMessage());
      updateLidarDataStatus(detection, EXTRACTION_ERROR);
    }
  }

  private void compute(
      String detectionIdentifier, List<FeatureWithDelimitation> featureWithDelimitations) {
    var roofGeometries = toGeometries(featureWithDelimitations);
    var roofsAnalysesResult = lidarRoofsAnalysisProcessor.apply(roofGeometries);
    var featuresWithDelimitationsWithRoofProperties =
        addRoofProperties(featureWithDelimitations, roofsAnalysesResult);

    // Clear cache as between process begin and end, detection may be updated
    entityManager.clear();
    var actualDetection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    detectionRepository.save(
        actualDetection.toBuilder()
            .featureWithDelimitations(featuresWithDelimitationsWithRoofProperties)
            .build());

    // Vgg
    Callable<Void> vggRequest =
        () -> {
          zoneVggRequestedService.accept(
              new FeatureVggRequested(
                  detectionIdentifier, actualDetection.getPolygonGeoJsonZone(), 0));
          return null;
        };

    // CityJSON
    Callable<Void> cityJsonRequest =
        () -> {
          detectionCityJSONGenerator.accept(actualDetection, roofsAnalysesResult);
          return null;
        };

    workers.invokeAll(List.of(vggRequest, cityJsonRequest));
  }

  private Set<Geometry> toGeometries(List<FeatureWithDelimitation> featureWithDelimitations) {
    Set<Feature> flattedFeatures =
        featureWithDelimitations.stream()
            .map(FeatureWithDelimitation::delimitations)
            .flatMap(List::stream)
            .collect(toSet());
    return flattedFeatures.stream().map(featureMapper::domainToGeometry).collect(toSet());
  }

  private List<FeatureWithDelimitation> addRoofProperties(
      List<FeatureWithDelimitation> featureWithDelimitations,
      RoofsAnalysisResult roofsAnalysisResult) {
    for (var featureWithDelimitation : featureWithDelimitations) {
      for (var delimitation : featureWithDelimitation.delimitations()) {
        if (delimitation.getProperties() == null) {
          delimitation.setProperties(new HashMap<>());
        }

        var properties = delimitation.getProperties();
        var roofProperties =
            roofsAnalysisResult.getProperties(featureMapper.domainToGeometry(delimitation));

        var planes = roofProperties.getPlanes();
        var firstPlane = planes.isEmpty() ? Plane3D.empty() : planes.getFirst();
        properties.put(ROOF_SLOPE_PROPERTY_NAME, firstPlane.getSlopeInDegrees().getValue());
        properties.put(ROOF_HEIGHT_PROPERTY_NAME, roofProperties.getHeightInMeters().getValue());
        properties.put(LIDAR_DATA_STATUS_PROPERTY_NAME, roofProperties.getData().status());
      }
    }

    return featureWithDelimitations;
  }

  private void updateLidarDataStatus(Detection detection, LidarDataStatus status) {
    var featureWithDelimitations = detection.getFeatureWithDelimitations();
    for (var featureWithDelimitation : featureWithDelimitations) {
      for (var delimitation : featureWithDelimitation.delimitations()) {
        if (delimitation.getProperties() == null) {
          delimitation.setProperties(new HashMap<>());
        }
        var properties = delimitation.getProperties();
        properties.put(LIDAR_DATA_STATUS_PROPERTY_NAME, status);
      }
    }

    entityManager.clear();
    var actualDetection = detectionRepository.findById(detection.getId()).orElseThrow();
    var actualDetectionWithNewStatus =
        actualDetection.toBuilder().featureWithDelimitations(featureWithDelimitations).build();

    detectionRepository.save(actualDetectionWithNewStatus);
  }
}
