package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.mapper.FileMapper;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@Slf4j
class GeoJsonContinuerControllerIT extends FacadeIT {
    @Autowired private GeoJsonContinuerController subject;
    @Autowired private ObjectMapper om;
    @Autowired private FileWriter fileWriter;

    @MockBean private GeoJsonContinuerService geoJsonContinuerService;
    @MockBean private FileMapper fileMapper;
    @MockBean private BucketComponent bucketComponent;

    private static final String GEOJSON_CONTENT = """
      {
        "type": "FeatureCollection",
        "features": [
          {
            "type": "Feature",
            "geometry": {
              "type": "Polygon",
              "coordinates": [[[0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 1.0], [0.0, 0.0]]]
            },
            "properties": {}
          }
        ]
      }
      """;

    @BeforeEach
    void setUp() {
        reset(geoJsonContinuerService, fileMapper, bucketComponent);
    }

    @Test
    void continue_geojson_ok() throws Exception {
        // Given
        var inputPolygon = mock(LatLonPolygon.class);
        var outputGeojson = mock(Geojson.class);
        var presignedUrl = new URL("https://fake-s3-url.com/output.geojson");

        when(fileMapper.apply(any(File.class))).thenReturn(inputPolygon);
        when(geoJsonContinuerService.continueGeojson(inputPolygon)).thenReturn(outputGeojson);
        doNothing().when(outputGeojson).saveAsFile(anyString());
        doNothing().when(bucketComponent).upload(any(File.class), anyString());
        when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(presignedUrl);

        // When
        var multipartFile = new MockMultipartFile(
                "file",
                "input.geojson",
                MediaType.APPLICATION_JSON_VALUE,
                GEOJSON_CONTENT.getBytes());

        var result = subject.continueGeoJson(multipartFile);

        // Then
        assertEquals(presignedUrl, result);
        verify(fileMapper).apply(any(File.class));
        verify(geoJsonContinuerService).continueGeojson(inputPolygon);
        verify(outputGeojson).saveAsFile(anyString());
        verify(bucketComponent).upload(any(File.class), anyString());
        verify(bucketComponent).presign(anyString(), eq(Duration.ofMinutes(10)));
    }

    @SneakyThrows
    @Test
    void continue_geojson_with_invalid_file_type_ko() {
        // Given
        var invalidFileBytes = new ClassPathResource("/shape/dummy.shape").getContentAsByteArray();
        var multipartFile = new MockMultipartFile(
                "file",
                "invalid.txt",
                MediaType.TEXT_PLAIN_VALUE,
                invalidFileBytes);

        // When - Then
        var exception = assertThrows(
                RuntimeException.class,
                () -> subject.continueGeoJson(multipartFile));

        assertTrue(exception.getMessage().contains("Invalid file type"));
        verifyNoInteractions(geoJsonContinuerService, fileMapper, bucketComponent);
    }

    @Test
    void continue_geojson_with_empty_file_ko() {
        // Given
        var multipartFile = new MockMultipartFile(
                "file",
                "empty.geojson",
                MediaType.APPLICATION_JSON_VALUE,
                new byte[0]);

        // When - Then
        var exception = assertThrows(
                RuntimeException.class,
                () -> subject.continueGeoJson(multipartFile));

        assertTrue(exception.getMessage().contains("empty"));
        verifyNoInteractions(geoJsonContinuerService, fileMapper, bucketComponent);
    }

    @SneakyThrows
    @Test
    void continue_geojson_with_real_file_ok() {
        // Given
        var geojsonFile = File.createTempFile("test", ".geojson");
        Files.writeString(geojsonFile.toPath(), GEOJSON_CONTENT);

        var inputPolygon = mock(LatLonPolygon.class);
        var outputGeojson = mock(Geojson.class);
        var presignedUrl = new URL("https://fake-s3-url.com/result.geojson");

        when(fileMapper.apply(any(File.class))).thenReturn(inputPolygon);
        when(geoJsonContinuerService.continueGeojson(inputPolygon)).thenReturn(outputGeojson);
        doNothing().when(outputGeojson).saveAsFile(anyString());
        doNothing().when(bucketComponent).upload(any(File.class), anyString());
        when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(presignedUrl);

        // When
        var multipartFile = new MockMultipartFile(
                "file",
                "valid.geojson",
                MediaType.APPLICATION_JSON_VALUE,
                Files.readAllBytes(geojsonFile.toPath()));

        var result = subject.continueGeoJson(multipartFile);

        // Then
        assertEquals(presignedUrl, result);
        verify(fileMapper).apply(any(File.class));
        verify(geoJsonContinuerService).continueGeojson(inputPolygon);
        verify(outputGeojson).saveAsFile(anyString());
        verify(bucketComponent).upload(any(File.class), anyString());
        verify(bucketComponent).presign(anyString(), eq(Duration.ofMinutes(10)));

        // Cleanup
        geojsonFile.delete();
    }
}