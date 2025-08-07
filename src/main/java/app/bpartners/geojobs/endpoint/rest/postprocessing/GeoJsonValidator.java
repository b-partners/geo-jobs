package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@AllArgsConstructor
public class GeoJsonValidator {
    private final ObjectMapper objectMapper;

    public boolean isValid(File file) {
        try {
            var inputContent = readFile(file);

            if (!"FeatureCollection".equals(inputContent.get("type").toString())) {
                return false;
            }

            var features = inputContent.get("features");
            if (!features.isArray() || features.isEmpty()) {
                return false;
            }

            for (JsonNode feature : features) {
                if(!"Feature".equals(feature.get("type").toString())) {
                    return false;
                }

                JsonNode geometry = feature.get("geometry");
                if(!geometry.isObject()) {
                    return false;
                }
                if(!"Polygon".equals(geometry.get("type").toString())) {
                    return false;
                }

                JsonNode coordinates = geometry.get("coordinates");
                if(!isValidPolygonCoordinates(coordinates)) {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValidPolygonCoordinates(JsonNode coordinates) {
        if(!coordinates.isArray() || coordinates.isEmpty()) {
            return false;
        }

        for(JsonNode ring: coordinates) {
            if(!ring.isArray() || ring.isEmpty() || !(ring.size() < 4)) {
                return false;
            }
        }

        return true;
    }

    public JsonNode readFile(File file) throws IOException {
        return  objectMapper.readTree(file);
    }
}
