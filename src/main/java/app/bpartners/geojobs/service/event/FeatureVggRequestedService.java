package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.service.*;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureVggRequestedService implements Consumer<FeatureVggRequested> {
  private static final int DEFAULT_TILE_SIZE = 1024;
  private final EntityManager entityManager;
  private final DetectionRepository detectionRepository;
  private final MachineDetectedTileRepository detectedTileRepository;
  private final VGGFactory vggFactory;
  private final DetectionVGGUpdate detectionVGGUpdate;
  private final TileCoordinatesService tileCoordinatesService;
  private final TiledPixelPolygonComputer tiledPixelPolygonComputer;
  private final FeaturePolygonRetriever featurePolygonRetriever;

  @Override
  public void accept(FeatureVggRequested event) {
    var featureVggComputationStart = now();
    entityManager.clear();
    var detectionIdentifier = event.getDetectionIdentifier();
    var feature = event.getFeature();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    if (!detection.hasToitureModelName()) {
      log.error("Only BP_TOITURE model is supported to generated VGG from now");
      return;
    }
    var featureWithDelimitationList = detection.getFeatureWithDelimitations();
    var actualDelimitation =
        filterDetectionDelimitationWithActualFeature(featureWithDelimitationList, feature);
    if (actualDelimitation == null) {
      throw new NoSuchElementException("No delimitation found for " + feature.getGeometry());
    }
    var geoJsonDelimitationType = detection.getGeoJsonDelimitationType();
    var polygonGeoJson = featurePolygonRetriever.apply(feature, geoJsonDelimitationType);
    if (polygonGeoJson == null) return;
    var detectableTypes =
        detection.getDetectableObjectConfigurations().stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .toList();
    var latLonRoofFeatures = actualDelimitation.getRestDelimitations();
    var machineDetectedTiles = detectedTileRepository.findAllByZdjJobId(detection.getZdjId());
    var tiledPixelPolygons =
        tiledPixelPolygonComputer.getTiledPixelPolygon(
            polygonGeoJson,
            latLonRoofFeatures,
            detectableTypes,
            machineDetectedTiles,
            detection.hasParcelDelimitationType());

    var completedQuadrilateralTileCoordinates =
        tileCoordinatesService.computeFeatureTileCoordinatesWithCompleteQuadrilateral(
            feature, geoJsonDelimitationType);

    var vggMap = vggFactory.from(tiledPixelPolygons, completedQuadrilateralTileCoordinates);

    var newDetection = detectionVGGUpdate.apply(vggMap.values(), detection, event.getFeatureNb());

    var tilesColNumbers = tileCoordinatesService.colNumbers(completedQuadrilateralTileCoordinates);
    var tileRowNumbers = tileCoordinatesService.rowNumbers(completedQuadrilateralTileCoordinates);
    var imageWidth = tilesColNumbers * DEFAULT_TILE_SIZE;
    var imageHeight = tileRowNumbers * DEFAULT_TILE_SIZE;

    detectionRepository.save(
        newDetection.toBuilder().imageWidth(imageWidth).imageHeight(imageHeight).build());

    log.info(
        "VGG computation finished in {} seconds for detection(e2Id={}) and feature(geometry={})",
        Duration.between(featureVggComputationStart, now()).toSeconds(),
        detection.getEndToEndId(),
        feature.getGeometry());
  }

  private FeatureWithDelimitation filterDetectionDelimitationWithActualFeature(
      List<FeatureWithDelimitation> featureWithDelimitationList, Feature feature) {
    return featureWithDelimitationList.stream()
        .filter(
            f ->
                f.getRestFeature() != null
                    && f.getRestFeature().getGeometry() != null
                    && f.getRestFeature().getGeometry().equals(feature.getGeometry()))
        .findFirst()
        .orElse(
            featureWithDelimitationList.size() == 1
                    && featureWithDelimitationList.getFirst().getRestDelimitations() != null
                    && featureWithDelimitationList.getFirst().getRestDelimitations().size() == 1
                ? featureWithDelimitationList.getFirst()
                : null);
  }
}
