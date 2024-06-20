package app.bpartners.geojobs.model;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record CommunityAuthorizationDetails(
    String id,
    @JsonProperty("community_name") String communityName,
    @JsonProperty("api_key") String apiKey,
    @JsonProperty("authorized_zone_names") List<String> authorizedZoneNames,
    @JsonProperty("detectable_objects_types") List<DetectableType> detectableObjectTypes) {}
