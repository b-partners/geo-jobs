package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.geojobs.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.AreaPicture;
import app.bpartners.geojobs.repository.model.AreaPictureMapLayer;
import app.bpartners.geojobs.service.AreaPictureRefresher;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureMapper {
  private final BucketComponent bucketComponent;
  private final AreaPictureRefresher areaPictureRefresher;

  public AreaPictureDetails toRest(
      AreaPicture domain, AreaPictureMapLayer actualLayer, List<AreaPictureMapLayer> otherLayers) {
    return new AreaPictureDetails()
        .id(domain.getId())
        .address(domain.getAddress())
        .userId(domain.getCommunityId())
        .fileUrl(bucketComponent.presign(domain.getFileKey()))
        .zoom(domain.getZoom())
        .currentTile(domain.getCurrentTile())
        .referenceTile(domain.getReferenceTile())
        .geoPosition(domain.getGeoPosition())
        .isExtended(domain.isExtended())
        .actualLayer(toRestMapLayer(actualLayer))
        .otherLayers(otherLayers.stream().map(this::toRestMapLayer).toList())
        .availableLayers(domain.getAvailableLayers())
        .createdAt(domain.getCreatedAt());
  }

  public AreaPicture toDomain(CrupdateAreaPictureDetails rest) {
    var areaPicture =
        AreaPicture.builder()
            .id(randomUUID().toString())
            .address(rest.getAddress())
            .communityId(null) // TODO: Roofer id retrieved from apiKey
            .fileKey(null)
            .zoom(rest.getZoom())
            .currentTile(null)
            .geoPosition(null)
            .isExtended(rest.getIsExtended())
            .availableLayers(List.of(TOUS_FR))
            .createdAt(rest.getCreatedAt())
            .build();
    return areaPictureRefresher.refreshTile(areaPicture);
  }

  private app.bpartners.geojobs.endpoint.rest.model.AreaPictureMapLayer toRestMapLayer(
      AreaPictureMapLayer domain) {
    return new app.bpartners.geojobs.endpoint.rest.model.AreaPictureMapLayer()
        .id(domain.getId())
        .departmentName(domain.getDepartmentName())
        .name(domain.getName())
        .year(domain.getYear())
        .precisionLevelInCm(domain.getPrecisionLevelInCm())
        .maximumZoom(domain.getMaxZoom())
        .maximumZoomLevel(domain.getMaxZoomLevel())
        .source(domain.getSource());
  }
}
