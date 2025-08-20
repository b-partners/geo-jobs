package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.GeoJsonContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonContinuation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class GeoJsonContinuerControllerIT extends FacadeIT {

    @Autowired private GeoJsonContinuerController subject;

    @MockBean private BucketComponent bucketComponent;
    @MockBean private EventProducer eventProducer;
    @MockBean private GeoJsonContinuationRepository repository;
    @MockBean private FileWriter fileWriter;

    private static byte[] fileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    @Test
    void test_continueGeoJson_existingFile() throws Exception {
        var resource = getClass().getResource("/amboditsiry/route-amboditsiry.geojson");
        assertNotNull(resource);

        var file = new File(resource.toURI());
        var fileBytes = fileToByteArray(file);

        var preSignedUrl = "https://mocked-s3-url.com/test-geojson-continued.geojson";

        var fakeContinuation = GeoJsonContinuation.builder()
                .id("test-id")
                .fileKey("continuations/geojson/test-id.geojson")
                .build();

        when(fileWriter.apply(any(byte[].class), any())).thenReturn(file);
        when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(URI.create(preSignedUrl).toURL());
        when(repository.findById(anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(fakeContinuation));
        when(bucketComponent.upload(any(File.class), anyString())).thenReturn(null);

        var actual = subject.continueGeoJson("test-id", fileBytes);

        assertNotNull(actual);
        assertEquals(preSignedUrl, actual);

        verify(eventProducer, times(1)).accept(any());
        verify(bucketComponent).presign(anyString(), any(Duration.class));
    }

    @Test
    void test_continueGeoJson_invalidFile() throws Exception {
        var resource = getClass().getResource("/shape/dummy.shape");
        assertNotNull(resource);

        var file = new File(resource.toURI());
        var fileBytes = fileToByteArray(file);

        assertThrows(
                Exception.class,
                () -> subject.continueGeoJson("test-id", fileBytes)
        );
    }
}
