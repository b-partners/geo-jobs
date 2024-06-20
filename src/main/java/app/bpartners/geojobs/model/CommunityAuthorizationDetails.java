package app.bpartners.geojobs.model;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CommunityAuthorizationDetails(
    String id,
    @JsonProperty("community_name") String communityName,
    @JsonProperty("api_key") String apiKey,
    @JsonProperty("detectable_objects_types") List<DetectableObjectType> detectableObjectTypes,
    @JsonProperty("authorized_zone_names") List<String> authorizedZoneNames) {}
