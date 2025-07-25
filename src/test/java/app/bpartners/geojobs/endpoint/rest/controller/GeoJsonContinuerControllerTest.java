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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import java.io.File;
import java.nio.file.Files;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GeoJsonContinuerController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)

@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GeoJsonContinuerControllerTest{

    @Autowired private MockMvc mockMvc;

    @MockBean private GeoJsonContinuerService geoJsonContinuerService;
    @MockBean private BucketComponent bucketComponent;

    @MockBean private FeatureMapper featureMapper;
    @MockBean private VGGFactory vggFactory;
    @MockBean private GeoJsonMapper geoJsonMapper;
    @MockBean private GeoJsonConverter geoJsonConverter;

    @Test
    void continueGeoJson_shouldReturnPresignedUrl() throws Exception {
        File geojsonFile = new File("src/test/resources/ivandry/route-ivandry.geojson");

        GeoJsonLoader loader = new GeoJsonLoader();
        Set<LatLonPolygon> polygons = loader.apply(geojsonFile);
        Geojson realGeojson = new Geojson(polygons);
        String expectedUrl = "http://presigned.url/result";

        when(geoJsonContinuerService.continueGeojson(any())).thenReturn(realGeojson);
        when(bucketComponent.presign(anyString())).thenReturn(expectedUrl);

        mockMvc.perform(multipart("/continue")
                        .file("file", Files.readAllBytes(geojsonFile.toPath())))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));
    }
}