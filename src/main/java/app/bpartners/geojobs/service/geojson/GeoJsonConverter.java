package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GeoJsonConverter implements Converter<List<HumanDetectedTile>, GeoJson> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;
  private final GeoJsonMapper mapper;

  @Override
  public GeoJson convert(List<HumanDetectedTile> detectedTiles) {
    List<GeoJson.GeoFeature> geoFeatures =
        detectedTiles.stream()
            .map(
                detectedTile -> {
                  var tile = detectedTile.getTile();
                  var xTile = tile.getCoordinates().getX();
                  var yTile = tile.getCoordinates().getY();
                  var zoom = tile.getCoordinates().getZ();
                  return mapper.toGeoFeatures(
                      xTile, yTile, zoom, DEFAULT_IMAGE_SIZE, detectedTile.getDetectedObjects());
                })
            .flatMap(List::stream)
            .toList();
    return new GeoJson(geoFeatures);
  }
}
