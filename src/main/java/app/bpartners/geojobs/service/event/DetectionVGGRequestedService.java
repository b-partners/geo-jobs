package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionVGGRequested;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.DetectionVGGUpdate;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

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
                  var polygonObjectTypesDeserialized =
                      tiledPixelPolygonSerializable.polygons().stream()
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

    var vgg = vggFactory.from(filteredTiledPixelPolygonsDeserialized);

    var newDetection = detectionVGGUpdate.apply(vgg, detection);

    detectionRepository.save(newDetection);
  }
}
