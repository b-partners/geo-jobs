package app.bpartners.geojobs.service.dashboard.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaPictureDetails(
    String id, AreaPictureMapLayer actualLayer, List<GeoPosition> geoPositions) {}
