package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Detection.GeoJsonDelimitationTypeEnum.ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectionFeatureType.PROVIDED_FEATURE;
import static app.bpartners.geojobs.service.geojson.GeoJsonMapper.convertPixelToGeographicalCoordinates;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureWithDetectionPropertiesRequested;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.service.FeatureDelimitationRetriever;
import app.bpartners.geojobs.service.FeatureRoofResultPropertiesComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geojson.GeometryCorrector;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureWithDetectionPropertiesRequestedService
    implements Consumer<FeatureWithDetectionPropertiesRequested> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;
  private final EntityManager entityManager;
  private final DetectionRepository detectionRepository;
  private final FeatureRoofResultPropertiesComputer featureRoofResultPropertiesComputer;
  private final GeometryConverter geometryConverter;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final FeatureDelimitationRetriever featureDelimitationRetriever;
  private final EventProducer eventProducer;
  private final GeometryCorrector geometryCorrector;

  @Override
  public void accept(FeatureWithDetectionPropertiesRequested event) {
    var detectionIdentifier = event.getDetectionIdentifier();
    var feature = event.getFeature();
    entityManager.clear();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    var savedDetection = apply(detection, feature);
    if (savedDetection != null) {
      eventProducer.accept(List.of(new FeatureVggRequested(detectionIdentifier, feature)));
    }
  }

  public app.bpartners.geojobs.repository.model.detection.Detection apply(
      app.bpartners.geojobs.repository.model.detection.Detection detection, Feature feature) {
    if (!detection.hasToitureModelName()) {
      log.error("Only BP_TOITURE model is supported to generated VGG from now");
      return null;
    }
    var geoJsonDelimitationType = detection.getGeoJsonDelimitationType();
    var latLonRoofGeometry =
        getLonLatGeometryIntersectedWithCurrentFeature(geoJsonDelimitationType, feature, detection);
    var detectedObjectPolygonGeometriesUsedForRateComputing =
        getDetectedObjectPolygonGeometriesUsedForRateComputing(
            detection.getZdjId(), latLonRoofGeometry);
    var computedProperties =
        featureRoofResultPropertiesComputer.apply(
            feature,
            latLonRoofGeometry,
            latLonRoofGeometry,
            detectedObjectPolygonGeometriesUsedForRateComputing);

    HashMap<String, Object> actualProperties = new HashMap<>();
    var featureProperties = feature.getProperties();
    if (featureProperties != null) {
      actualProperties.putAll(featureProperties);
    }
    actualProperties.putAll(computedProperties);

    detection.addFeatures(
        List.of(
            toDomainFeature(
                new Feature()
                    .type(feature.getType())
                    .properties(actualProperties)
                    .geometry(feature.getGeometry()))),
        PROVIDED_FEATURE);

    return detectionRepository.save(detection);
  }

  private List<PolygonObjectType> getDetectedObjectPolygonGeometriesUsedForRateComputing(
      String machineZDJIdentifier, Geometry latLonDelimitationObjectType) {
    var machineDetectedTiles =
        machineDetectedTileRepository.findAllByZdjJobId(machineZDJIdentifier);
    return machineDetectedTiles.stream()
        .map(
            machineDetectedTile ->
                DetectedTile.builder()
                    .tile(machineDetectedTile.getTile())
                    .detectedObjects(machineDetectedTile.getDetectedObjects())
                    .build())
        .map(
            detectedTile -> {
              var tile = detectedTile.getTile();
              var xTile = tile.getCoordinates().getX();
              var yTile = tile.getCoordinates().getY();
              var zoom = tile.getCoordinates().getZ();
              return detectedTile.getDetectedObjects().stream()
                  .map(
                      detectedObject -> {
                        var multiPolygonDetectedObject =
                            getMultiPolygonFromRestFeatureGeometryInstance(
                                detectedObject.getFeature().getGeometry().getActualInstance());
                        var multiPolygonDetectedObjectCoordinates =
                            multiPolygonDetectedObject.getCoordinates();
                        var latLonCoordinates =
                            convertPixelToGeographicalCoordinates(
                                xTile,
                                yTile,
                                zoom,
                                DEFAULT_IMAGE_SIZE,
                                multiPolygonDetectedObjectCoordinates);
                        var detectableObjectType = detectedObject.getDetectableObjectType();
                        var latLonMultiPolygonDetectedObject =
                            geometryConverter.apply(latLonCoordinates);

                        var correctedLatLonDetectedObjectGeometry =
                            geometryCorrector.apply(latLonMultiPolygonDetectedObject);

                        var intersectionBetweenDetectedObjectAndDelimitationObjectType =
                            latLonDelimitationObjectType.intersection(
                                correctedLatLonDetectedObjectGeometry);
                        if (intersectionBetweenDetectedObjectAndDelimitationObjectType.isEmpty()) {
                          return null;
                        }
                        List<PolygonObjectType> polygonObjectTypes = new java.util.ArrayList<>();
                        if (intersectionBetweenDetectedObjectAndDelimitationObjectType
                            instanceof org.locationtech.jts.geom.Polygon polygon) {
                          polygonObjectTypes.add(
                              new PolygonObjectType(polygon, detectableObjectType));
                        } else if (intersectionBetweenDetectedObjectAndDelimitationObjectType
                            instanceof org.locationtech.jts.geom.MultiPolygon multiPolygon) {
                          for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            polygonObjectTypes.add(
                                new PolygonObjectType(
                                    (org.locationtech.jts.geom.Polygon)
                                        multiPolygon.getGeometryN(i),
                                    detectableObjectType));
                          }
                        }
                        return polygonObjectTypes;
                      })
                  .filter(Objects::nonNull)
                  .flatMap(List::stream)
                  .toList();
            })
        .flatMap(List::stream)
        .toList();
  }

  private Geometry getLonLatGeometryIntersectedWithCurrentFeature(
      Detection.GeoJsonDelimitationTypeEnum geoJsonDelimitationType,
      Feature feature,
      app.bpartners.geojobs.repository.model.detection.Detection detection) {
    Geometry latLonRoofGeometry;
    if (ROOF.equals(geoJsonDelimitationType)) {
      var geometryInstance = feature.getGeometry().getActualInstance();
      var multiPolygon = getMultiPolygonFromRestFeatureGeometryInstance(geometryInstance);
      latLonRoofGeometry = geometryConverter.apply(multiPolygon.getCoordinates());
    } else {
      var featureWithDelimitationList = detection.getFeatureWithDelimitations();
      var actualDelimitation =
          featureDelimitationRetriever.apply(featureWithDelimitationList, feature);
      if (actualDelimitation == null) {
        throw new NoSuchElementException("No delimitation found for " + feature.getGeometry());
      }
      var unifiedDelimitationMultiPolygon =
          actualDelimitation.getRestDelimitations().stream()
              .map(
                  f ->
                      getMultiPolygonFromRestFeatureGeometryInstance(
                          f.getGeometry().getActualInstance()))
              .map(m -> geometryConverter.apply(m.getCoordinates()))
              .reduce(unifyMultiPolygon())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Unable to compute delimitations multipolygon while computing feature"
                              + " properties with detection result"));
      var featureMultiPolygonGeometry =
          geometryConverter.apply(
              getMultiPolygonFromRestFeatureGeometryInstance(
                      actualDelimitation.getRestFeature().getGeometry().getActualInstance())
                  .getCoordinates());
      var intersectionBetweenFeatureMultiPolygonAndDelimitationMultiPolygon =
          unifiedDelimitationMultiPolygon.intersection(featureMultiPolygonGeometry);
      if (intersectionBetweenFeatureMultiPolygonAndDelimitationMultiPolygon.isEmpty()) {
        throw new IllegalStateException("No intersection between feature and delimitation");
      }
      latLonRoofGeometry = intersectionBetweenFeatureMultiPolygonAndDelimitationMultiPolygon;
    }
    return latLonRoofGeometry;
  }

  private MultiPolygon getMultiPolygonFromRestFeatureGeometryInstance(Object geometryInstance) {
    if (geometryInstance instanceof Polygon polygon) {
      return new MultiPolygon().type(MULTI_POLYGON).coordinates(List.of(polygon.getCoordinates()));
    } else if (geometryInstance instanceof MultiPolygon m) {
      return m;
    } else {
      throw new IllegalArgumentException(
          "Unsupported geometry type " + geometryInstance.getClass());
    }
  }
}
