package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.utils.lidar.LidarRoofsAnalysisProcessorCreator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class DetectionCityJSONGeneratorIT extends FacadeIT {
  @MockBean private BucketComponent bucketComponentMock;
  @Autowired private DetectionRepository detectionRepository;
  @Autowired private FeatureMapper featureMapper;
  @Autowired private DetectionCityJSONGenerator subject;
  private static final LidarRoofsAnalysisProcessorCreator processorCreator =
      new LidarRoofsAnalysisProcessorCreator();

  private static final String DETECTION_ID = randomUUID().toString();
  private static final String FEATURE_WITH_DELIMITATION_ID = randomUUID().toString();

  @BeforeEach()
  void setUp() {
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock());

    detectionRepository.save(detection());
  }

  @AfterEach
  void cleanUp() {
    detectionRepository.deleteById(DETECTION_ID);
  }

  @Test
  void generate_cityjson_ok() {
    var roofsGeometries = Set.of(roofGeometry1());
    var processor =
        processorCreator.create(
            roofsGeometries,
            processorCreator.createTempFileFromResources(
                "las/LHD_FXX_0644_6859_PTS_O_LAMB93_IGN69.copc.laz"));

    var analysisResult = processor.apply(roofsGeometries);

    subject.accept(detection(), analysisResult);

    var actualDetection = detectionRepository.findById(DETECTION_ID).orElseThrow();

    assertEquals(1, actualDetection.getCityJsons().size());
  }

  private FeatureWithDelimitation featureWithDelimitation() {
    var restFeature =
        featureMapper.toRest((Polygon) roofGeometry1(), 20, FEATURE_WITH_DELIMITATION_ID);
    var domainFeature = toDomainFeature(restFeature);
    return new FeatureWithDelimitation(domainFeature, List.of(domainFeature));
  }

  private Detection detection() {
    return Detection.builder()
        .id(DETECTION_ID)
        .endToEndId(DETECTION_ID)
        .featureWithDelimitations(List.of(featureWithDelimitation()))
        .toNotify(false)
        .creationDatetime(now())
        .build();
  }

  private static Geometry roofGeometry1() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }
}
