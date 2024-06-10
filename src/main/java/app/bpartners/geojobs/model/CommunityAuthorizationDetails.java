package app.bpartners.geojobs.model;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CommunityAuthorizationDetails(
    String id,
    @JsonProperty("community_name") String communityName,
    @JsonProperty("api_key") String apiKey,
    @JsonProperty("detectable_objects") List<DetectableObjectType> detectableObjects,
    @JsonProperty("total_accessible_zone") MultiPolygon totalAccessibleZone) {}
