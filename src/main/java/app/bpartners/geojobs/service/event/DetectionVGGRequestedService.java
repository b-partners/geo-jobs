package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.event.model.DetectionVGGRequested;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.DetectionVGGUpdate;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionVGGRequestedService implements Consumer<DetectionVGGRequested> {
  private final DetectionRepository detectionRepository;
  private final VGGFactory vggFactory;
  private final DetectionVGGUpdate detectionVGGUpdate;
  private final GeometryConverter geometryConverter;

  @Override
  public void accept(DetectionVGGRequested event) {
    var detectionId = event.getDetectionId();
    var detection = detectionRepository.findById(detectionId).orElseThrow();
    var filteredTiledPixelPolygons = event.getFilteredTiledPixelPolygons();
    var filteredTiledPixelPolygonsDeserialized =
        filteredTiledPixelPolygons.stream()
            .map(
                tiledPixelPolygonSerializable -> {
                  var serializedPolygons = tiledPixelPolygonSerializable.polygons();
                  var polygonObjectTypesDeserialized =
                      serializedPolygons.stream()
                          .map(
                              polygonObjectTypeSerializable -> {
                                var geometry =
                                    geometryConverter.readGeometryFromString(
                                        polygonObjectTypeSerializable.polygonAsString());
                                if (geometry instanceof Polygon polygon) {
                                  return new PolygonObjectType(
                                      polygon, polygonObjectTypeSerializable.detectableType());
                                }
                                return null;
                              })
                          .filter(Objects::nonNull)
                          .toList();
                  return new TiledPixelPolygon(
                      tiledPixelPolygonSerializable.point(),
                      polygonObjectTypesDeserialized,
                      tiledPixelPolygonSerializable.tileX(),
                      tiledPixelPolygonSerializable.tileY(),
                      tiledPixelPolygonSerializable.zoom());
                })
            .toList();

    var latLonRoofPolygon =
        detection.getProvidedGeoJsonZone().stream()
            .map(
                feature -> {
                  var geometryType = feature.getGeometry().getActualInstance();
                  MultiPolygon multiPolygon;
                  switch (geometryType) {
                    case app.bpartners.geojobs.endpoint.rest.model.Polygon restPolygon ->
                        multiPolygon =
                            geometryConverter.apply(List.of(restPolygon.getCoordinates()));
                    case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon restMultiPolygon ->
                        multiPolygon = geometryConverter.apply(restMultiPolygon.getCoordinates());
                    default ->
                        throw new IllegalStateException(
                            "Unexpected geometry type: " + geometryType);
                  }
                  return multiPolygon;
                })
            .reduce(unifyMultiPolygon())
            .orElseThrow(() -> new NotFoundException("No roof polygon found for provided zone"));

    var featureVgg = vggFactory.from(filteredTiledPixelPolygonsDeserialized, latLonRoofPolygon);

    var newDetection = detectionVGGUpdate.apply(featureVgg, detection);

    detectionRepository.save(newDetection);
  }
}
