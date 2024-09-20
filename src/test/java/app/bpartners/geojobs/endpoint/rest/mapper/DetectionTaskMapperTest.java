package app.bpartners.geojobs.endpoint.rest.mapper;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionTaskMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectedObject;
import app.bpartners.geojobs.endpoint.rest.model.DetectedParcel;
import app.bpartners.geojobs.endpoint.rest.model.DetectedTile;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionTaskMapperTest {
  DetectedTileRepository detectedTileRepositoryMock = mock();
  DetectionTaskMapper subject = new DetectionTaskMapper(detectedTileRepositoryMock);

  @Test
  void map_with_detected_tile_ok() {
    var parcelId = randomUUID().toString();
    var jobId = randomUUID().toString();
    var tile = Tile.builder().id(randomUUID().toString()).creationDatetime(now()).build();
    MachineDetectedTile machineDetectedTile =
        MachineDetectedTile.builder()
            .tile(tile)
            .detectedObjects(
                List.of(
                    someDetectedObject(PANNEAU_PHOTOVOLTAIQUE),
                    someDetectedObject(TOITURE_REVETEMENT),
                    someDetectedObject(ARBRE),
                    someDetectedObject(PISCINE),
                    someDetectedObject(PASSAGE_PIETON)))
            .creationDatetime(now())
            .build();
    ParcelContent parcelContentMock = mock();
    when(detectedTileRepositoryMock.findAllByParcelId(parcelId))
        .thenReturn(List.of(machineDetectedTile));

    DetectedParcel actual =
        subject.toRest(
            jobId, Parcel.builder().id(parcelId).parcelContent(parcelContentMock).build());

    assertEquals(
        new DetectedParcel()
            .id(actual.getId())
            .detectionJobIb(actual.getDetectionJobIb())
            .parcelId(parcelId)
            .creationDatetime(actual.getCreationDatetime())
            .detectedTiles(
                List.of(
                    new DetectedTile()
                        .tileId(tile.getId())
                        .creationDatetime(tile.getCreationDatetime())
                        .detectedObjects(
                            List.of(
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .PANNEAU_PHOTOVOLTAIQUE),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .TOITURE_REVETEMENT),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .ARBRE),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .PISCINE),
                                someRestDetectedObject(
                                    app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType
                                        .PASSAGE_PIETON))))),
        actual);
  }

  private static DetectedObject someRestDetectedObject(
      app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType restObjectType) {
    return new DetectedObject()
        .confidence(BigDecimal.valueOf(1.0))
        .detectedObjectType(restObjectType)
        .detectorVersion("TODO");
  }

  private app.bpartners.geojobs.repository.model.detection.DetectedObject someDetectedObject(
      DetectableType detectableType) {
    return app.bpartners.geojobs.repository.model.detection.DetectedObject.builder()
        .detectedObjectType(DetectableObjectType.builder().detectableType(detectableType).build())
        .computedConfidence(1.0)
        .build();
  }
}
