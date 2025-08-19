package app.bpartners.geojobs.endpoint.rest.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@AllArgsConstructor
public class FileGeoJsonValidator implements Consumer<MultipartFile> {

    private final ObjectMapper objectMapper;

    /**
     * Vérifie si le GeoJSON est valide pour contenir des Polygones.
     */
    boolean isValid(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }

        try {
            JsonNode root = readFile(file);

            if (!"FeatureCollection".equals(root.path("type").asText())) {
                return false;
            }

            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                return false;
            }

            for (JsonNode feature : features) {
                if (!"Feature".equals(feature.path("type").asText())) {
                    return false;
                }

                JsonNode geometry = feature.path("geometry");
                if (geometry.isMissingNode() || !geometry.isObject()) {
                    return false;
                }

                if (!"Polygon".equals(geometry.path("type").asText())) {
                    return false;
                }

                JsonNode coordinates = geometry.path("coordinates");
                if (!isValidPolygonCoordinates(coordinates)) {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            throw new RuntimeException("Erreur de lecture du fichier GeoJSON", e);
        }
    }

    /**
     * Vérifie que chaque anneau de polygone est correct.
     */
    private boolean isValidPolygonCoordinates(JsonNode coordinates) {
        if (!coordinates.isArray() || coordinates.isEmpty()) {
            return false;
        }

        for (JsonNode ring : coordinates) {
            if (!ring.isArray() || ring.size() < 4) {
                return false;
            }

            JsonNode first = ring.get(0);
            JsonNode last = ring.get(ring.size() - 1);
            if (!ringEquals(first, last)) {
                return false;
            }

            for (JsonNode point : ring) {
                if (!point.isArray() || point.size() < 2) {
                    return false;
                }

                double lon = point.get(0).asDouble();
                double lat = point.get(1).asDouble();
                if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean ringEquals(JsonNode first, JsonNode last) {
        return first.get(0).asDouble() == last.get(0).asDouble() &&
                first.get(1).asDouble() == last.get(1).asDouble();
    }

    JsonNode readFile(MultipartFile file) throws IOException {
        return objectMapper.readTree(file.getInputStream());
    }

    @Override
    public void accept(MultipartFile multipartFile) {
        if (!isValid(multipartFile)) {
            throw new RuntimeException("Fichier GeoJSON invalide : doit contenir uniquement des polygones valides");
        }
    }
}
