package app.bpartners.geojobs.endpoint.rest.controller;


import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
public class GeoJsonContinuerControllerTestIT {
    private final BucketComponent  bucketComponentMock = Mockito.mock(BucketComponent.class);
    private final GeoJsonContinuerService geoJsonContinuerServiceMock = Mockito.mock(GeoJsonContinuerService.class);
    private final GeoJsonContinuerController controller = new GeoJsonContinuerController(geoJsonContinuerServiceMock, bucketComponentMock);


    @Test
    void continueGeoJson_shouldReturnPresignedUrl() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "input.geojson", "application/json", "{}".getBytes()
        );

        var mockResult = mock(Geojson.class);

        when(geoJsonContinuerServiceMock.continueGeojson(any(File.class))).thenReturn(mockResult);
        when(bucketComponentMock.presign(anyString()))
                .thenReturn("https://url-presigned");

        String resultUrl = controller.continueGeoJson(file);

        verify(bucketComponentMock).upload(any(File.class), anyString());
        verify(bucketComponentMock).presign(anyString());

        assertEquals("https://url-presigned", resultUrl);
    }
}