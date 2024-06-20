package app.bpartners.geojobs.model;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record CommunityAuthorizationDetails(
    String id,
    @JsonProperty("community_name") String communityName,
    @JsonProperty("api_key") String apiKey,
    @JsonProperty("authorized_zone_names") List<String> authorizedZoneNames,
    @JsonProperty("detectable_objects_types") List<DetectableObjectType> detectableObjectTypes) {}
