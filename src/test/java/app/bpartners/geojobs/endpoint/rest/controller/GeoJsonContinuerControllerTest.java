package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GeoJsonContinuerController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GeoJsonContinuerControllerTest extends FacadeIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private GeoJsonContinuerService geoJsonContinuerService;
    @MockBean private BucketComponent bucketComponent;

    // Ajoutez ces mocks pour les nouvelles dépendances manquantes
    @MockBean private FeatureMapper featureMapper;
    @MockBean private VGGFactory vggFactory;
    @MockBean private GeoJsonMapper geoJsonMapper; // Nouveau mock ajouté
    @MockBean private GeoJsonConverter geoJsonConverter; // Si nécessaire

    @Test
    void continueGeoJson_shouldReturnPresignedUrl() throws Exception {
        GeoJsonLoader loader = new GeoJsonLoader();
        File geojsonFile = new File("src/test/resources/ivandry/route-ivandry.geojson");
        Set<LatLonPolygon> polygons = loader.apply(geojsonFile);
        Geojson realGeojson = new Geojson(polygons);
        String expectedUrl = "http://presigned.url/result";

        // Mocking service methods
        when(geoJsonContinuerService.continueGeojson(any())).thenReturn(realGeojson);
        when(bucketComponent.presign(anyString())).thenReturn(expectedUrl);

        // On lit le vrai contenu JSON depuis le fichier (comme dans une vraie requête)
        String rawGeoJson = new String(Files.readAllBytes(Paths.get(geojsonFile.toURI())));

        // Exécution de la requête
        mockMvc.perform(post("/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawGeoJson))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));
    }
}