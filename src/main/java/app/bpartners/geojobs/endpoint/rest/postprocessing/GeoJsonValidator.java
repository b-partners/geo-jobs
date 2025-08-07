package app.bpartners.geojobs.endpoint.rest.postprocessing;

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

            if (file == null || file.isEmpty()) {
                return false;
            }

            if (!"FeatureCollection".equals(inputContent.get("type").toString())) {
                return false;
            }

            var features = inputContent.get("features");
            if (!features.isArray() || features.isEmpty()) {
                return false;
            }

            for (JsonNode feature : features) {
                if (!"Feature".equals(feature.get("type").toString())) {
                    return false;
                }

                JsonNode geometry = feature.get("geometry");
                if (!geometry.isObject()) {
                    return false;
                }
                if (!"Polygon".equals(geometry.get("type").toString())) {
                    return false;
                }

                JsonNode coordinates = geometry.get("coordinates");
                if (!isValidPolygonCoordinates(coordinates)) {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValidPolygonCoordinates(JsonNode coordinates) {
        if (!coordinates.isArray() || coordinates.isEmpty()) {
            return false;
        }

        for (JsonNode ring : coordinates) {
            if (!ring.isArray() || ring.isEmpty() || !(ring.size() < 4)) {
                return false;
            }

            JsonNode firstRing = ring.get(0);
            JsonNode lastRing = ring.get(ring.size() - 1);

            if (!ringEquals(firstRing, lastRing)) {
                return false;
            }

            for (JsonNode ringPoint : ring) {
                if (!ringPoint.isArray() || ringPoint.isEmpty() || ringPoint.size() < 2) {
                    return false;
                }

                double lon = ringPoint.get(0).asDouble();
                double lat = ringPoint.get(1).asDouble();
                if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean ringEquals(JsonNode firstRing, JsonNode lastRing) {
        if (firstRing.size() < 2 && lastRing.size() < 2) {
            return false;
        }

        return firstRing.get(0).asDouble() == lastRing.get(0).asDouble()
                && firstRing.get(1).asDouble() == lastRing.get(1).asDouble();
    }

    public JsonNode readFile(File file) throws IOException {
        return objectMapper.readTree(file);
    }
}
