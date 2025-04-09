package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.RooferMadeDetectionCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.detection.DetectionTilingCreation;
import app.bpartners.geojobs.service.detection.DetectionTilingStatisticsComputer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.RooferMadeTilingService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import app.bpartners.geojobs.utils.FeatureCreator;
import org.junit.jupiter.api.Test;

public class DetectionTilingCreationTest {
  FeatureCreator featureCreator = new FeatureCreator();
  ZoneTilingJobMapper zoneTilingJobMapper = mock();
  ZoneTilingJobService zoneTilingJobService = mock();
  DetectionRepository detectionRepository;
  DetectionTilingStatisticsComputer detectionTilingStatisticsComputer = mock();
  RooferMadeTilingService rooferMadeTilingService = mock();
  EventProducer<RooferMadeDetectionCreated> eventProducer = mock();
  ZoneDetectionJobService zoneDetectionJobService = mock();
  FeatureMapper featureMapper = new FeatureMapper();
  DetectionTilingCreation subject =
      new DetectionTilingCreation(
          zoneTilingJobMapper,
          zoneTilingJobService,
          detectionRepository,
          detectionTilingStatisticsComputer,
          rooferMadeTilingService,
          eventProducer,
          zoneDetectionJobService,
          featureMapper);

  @Test
  void extend_features_polygons() {
    var actual = subject.extend(featureCreator.defaultFeatures());

    assertNotEquals(featureCreator.defaultFeatures(), actual);
  }
}
