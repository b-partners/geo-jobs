package app.bpartners.geojobs.service.area.mutation;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.DetectionMaskFromTileRetriever;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.net.URI;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

/** Builds the {@link MutationContext} used to detect a roof mutation for a detection. */
@Component
@RequiredArgsConstructor
public class MutationContextFactory {
  private final DetectionMaskFromTileRetriever maskFromTileRetriever;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final GeometryConverter geometryConverter;

  public MutationContext create(Detection detection, Geometry roofGeometry) {
    // TODO: re-enable once parcel delimitations are grouped by image date (millésime), see
    // MutationComputer.getMostRecentInstantParcel/getPrecedentInstantParcel
    return null;
  }

  private Tile findTileIntersecting(Detection detection, MultiPolygon roofMultiPolygon) {
    return machineDetectedTileRepository.findAllByZdjJobId(detection.getZdjId()).stream()
        .map(MachineDetectedTile::getTile)
        .filter(tile -> tileMultiPolygon(tile).intersects(roofMultiPolygon))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No tile intersects the roof to compute the mutation for detection "
                        + detection.getId()));
  }

  private MultiPolygon tileMultiPolygon(Tile tile) {
    var tileCoordinates = tile.getCoordinates();
    return geometryConverter.getMultiPolygonFromTile(
        tileCoordinates.getX(), tileCoordinates.getY(), tileCoordinates.getZ());
  }

  private static MultiPolygon asMultiPolygon(Geometry geometry) {
    if (geometry instanceof MultiPolygon multiPolygon) {
      return multiPolygon;
    }
    if (geometry instanceof Polygon polygon) {
      return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
    }
    throw new IllegalArgumentException(
        "Unsupported geometry type to build the mutation mask: " + geometry.getClass());
  }

  @SneakyThrows
  private static URL toUrl(String geoServerUrl) {
    return new URI(geoServerUrl).toURL();
  }
}
