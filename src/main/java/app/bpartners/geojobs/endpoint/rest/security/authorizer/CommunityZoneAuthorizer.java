package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizedZone;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityZoneAuthorizer implements BiConsumer<CommunityAuthorization, List<Feature>> {
  private final FeatureMapper featureMapper;

  @Override
  public void accept(
      CommunityAuthorization communityAuthorization, List<Feature> candidateFeatures) {
    var candidateFeaturesPolygon =
        candidateFeatures.stream()
            .map(featureMapper::toDomain)
            .reduce((acc, feature) -> (Polygon) acc.union(feature));

    var authorizedZonePolygon =
        communityAuthorization.getAuthorizedZones().stream()
            .map(CommunityAuthorizedZone::getMultiPolygon)
            .map(this::convertPolygonToFeature)
            .map(featureMapper::toDomain)
            .reduce((acc, feature) -> (Polygon) acc.union(feature))
            .orElseThrow(
                () ->
                    new ForbiddenException(
                        "There is no zone authorized for your community.name="
                            + communityAuthorization.getName()));

    if (candidateFeaturesPolygon.isPresent()
        && !authorizedZonePolygon.contains(candidateFeaturesPolygon.get())) {
      throw new ForbiddenException(
          "Some given feature is not allowed for your community.name = "
              + communityAuthorization.getName());
    }
  }

  private Feature convertPolygonToFeature(MultiPolygon multiPolygon) {
    var feature = new Feature();
    feature.setGeometry(multiPolygon);
    return feature;
  }
}
