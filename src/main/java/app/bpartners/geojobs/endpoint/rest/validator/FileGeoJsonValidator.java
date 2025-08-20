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
     * Vérifie que le fichier GeoJSON contient uniquement de polygones valides.
     */
    boolean isValid(MultipartFile file) {
        if (file.isEmpty()) return false;
        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            if (!"FeatureCollection".equals(root.path("type").asText())) return false;
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) return false;
            for (JsonNode feature : features) {
                if (!"Feature".equals(feature.path("type").asText())) return false;
                JsonNode geometry = feature.path("geometry");
                if (geometry.isMissingNode() || !geometry.isObject()) return false;
                if (!"Polygon".equals(geometry.path("type").asText())) return false;
                if (!isValidPolygonCoordinates(geometry.path("coordinates"))) return false;
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Erreur de lecture du fichier GeoJSON", e);
        }
    }

    /**
     * Vérifie la validité des coordonnées
     */
    private boolean isValidPolygonCoordinates(JsonNode coordinates) {
        if (!coordinates.isArray() || coordinates.isEmpty()) return false;
        for (JsonNode ring : coordinates) {
            if (!ring.isArray() || ring.size() < 4) return false;
            JsonNode first = ring.get(0), last = ring.get(ring.size() - 1);
            if (!ringEquals(first, last)) return false;
            for (JsonNode point : ring) {
                if (!point.isArray() || point.size() < 2) return false;
                double lon = point.get(0).asDouble(), lat = point.get(1).asDouble();
                if (lon < -180 || lon > 180 || lat < -90 || lat > 90) return false;
            }
        }
        return true;
    }

    /**
     * Vérifie que le premier et le dernier point d'un anneau sont identiques.
     */
    private boolean ringEquals(JsonNode first, JsonNode last) {
        return first.get(0).asDouble() == last.get(0).asDouble() &&
                first.get(1).asDouble() == last.get(1).asDouble();
    }

    @Override
    public void accept(MultipartFile multipartFile) {
        if (!isValid(multipartFile)) {
            throw new RuntimeException("Fichier GeoJSON invalide : doit contenir uniquement des polygones valides");
        }
    }
}