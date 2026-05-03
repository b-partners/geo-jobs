package app.bpartners.geojobs.service.cityjson.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record TexturedBuildingData(
    String id,
    List<TexturedGeometry> roofs,
    List<TexturedGeometry> walls,
    List<TexturedGeometry> grounds,
    Map<String, Object> properties,
    String textureDataUri
) {}
