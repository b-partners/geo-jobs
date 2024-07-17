package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.unit.GeoJsonMapperTest.detectedObject;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionInitiated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class GeoJsonConversionInitiatedServiceIT extends FacadeIT {
  private static final String MOCK_ANNOTATED_JOB_ID = "mock_job_id";
  @MockBean private HumanDetectionJobRepository humanDetectionJobRepository;
  @Autowired private GeoJsonConverter geoJsonConverter;
  @Autowired private FileWriter writer;
  @Autowired private HTMLTemplateParser templateParser;
  @MockBean private Mailer mailer;
  @Autowired private GeoJsonConversionInitiatedService subject;

  HumanDetectionJob humanDetectionJob() {
    return HumanDetectionJob.builder()
        .id(randomUUID().toString())
        .detectedTiles(
            List.of(
                DetectedTile.builder()
                    .tile(
                        Tile.builder()
                            .coordinates(new TileCoordinates().x(521151).y(151151).z(20))
                            .build())
                    .detectedObjects(List.of(detectedObject()))
                    .build()))
        .build();
  }

  GeoJsonConversionInitiated initiated() {
    return new GeoJsonConversionInitiated(MOCK_ANNOTATED_JOB_ID, "tech@bpartners.app");
  }

  @BeforeEach
  void setUp() {
    when(humanDetectionJobRepository.findByAnnotationJobId(any()))
        .thenReturn(Optional.of(humanDetectionJob()));
  }

  @Test
  void generate_geo_json_from_detected_tiles() {
    subject.accept(initiated());

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer, times(1)).accept(emailCaptor.capture());
  }
}
