package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.geojson.GeoReferencer.getFranceDepartmentsSimpleFeaturesMatchingPredicate;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AreaPictureMapLayerRepository;
import app.bpartners.geojobs.repository.model.AreaPicture;
import app.bpartners.geojobs.repository.model.AreaPictureMapLayer;
import java.util.List;
import lombok.AllArgsConstructor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AreaPictureMapLayerService {
  private final AreaPictureMapLayerRepository repository;
  public static final String DEFAULT_IGN_LAYER_UUID = "1cccfc17-cbef-4320-bdfa-0d1920b91f11";

  public AreaPictureMapLayer getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new NotFoundException("AreaPictureMapLayer.id=" + id + " is not found."));
  }

  public List<AreaPictureMapLayer> findOtherLayers(AreaPicture areaPicture) {
    return getAvailableLayersFrom(areaPicture.getGeoPositionAsPoint());
  }

  private List<AreaPictureMapLayer> getAvailableLayersFrom(Point geoPosition) {
    List<SimpleFeature> features =
        getFranceDepartmentsSimpleFeaturesMatchingPredicate(
            feature -> {
              var geometry = (Geometry) feature.getDefaultGeometry();
              return geometry.contains(geoPosition);
            });
    if (features.isEmpty()) {
      return List.of(getDefaultIGNLayer());
    }
    List<String> matchingFeaturesName =
        features.stream().map(f -> (String) f.getAttribute("nom")).toList();
    var layers = repository.findAreaPictureMapLayerByDepartmentNameIgnoreCase(matchingFeaturesName);

    if (layers.isEmpty()) {
      return List.of(getDefaultIGNLayer());
    }
    return layers;
  }

  public AreaPictureMapLayer getDefaultIGNLayer() {
    return getById(DEFAULT_IGN_LAYER_UUID);
  }
}
