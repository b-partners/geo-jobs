package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonContinuerIsCompleted;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.GeoJsonContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonContinuation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import app.bpartners.geojobs.service.event.GeoJsonContinuerRequestedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class GeoJsonContinuerRequestedServiceIT extends FacadeIT {

    @Autowired
    private GeoJsonContinuerRequestedService subject;

    @MockBean private BucketComponent bucketComponent;
    @MockBean private FileWriter fileWriter;
    @MockBean private GeoJsonContinuationRepository repository;

    private static byte[] fileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    @Test
    void test_accept_processesGeoJsonSuccessfully() throws Exception {
        var resource = getClass().getResource("/amboditsiry/route-amboditsiry.geojson");
        assertNotNull(resource);

        var file = new File(resource.toURI());
        var fileBytes = fileToByteArray(file);

        final String fileKey = "continuations/geojson/amboditsiry.geojson";
        final String id = "amboditsiry123";

        when(bucketComponent.download(fileKey)).thenReturn(file);
        when(fileWriter.writeAsByte(any(Geojson.class))).thenReturn(fileBytes);
        when(fileWriter.apply(any(byte[].class), any())).thenReturn(file);
        when(repository.save(any(GeoJsonContinuation.class))).thenReturn(null);
        when(bucketComponent.upload(any(File.class), anyString())).thenReturn(null);

        var event = new GeoJsonContinuerIsCompleted(id, fileKey);

        subject.accept(event);

        verify(bucketComponent).download(fileKey);
        verify(fileWriter).writeAsByte(any(Geojson.class));
        verify(fileWriter).apply(any(byte[].class), any());
        verify(bucketComponent).upload(any(File.class), eq(fileKey));
        verify(repository).save(any(GeoJsonContinuation.class));
    }

    @Test
    void test_continueGeojson_returnsGeojsonObject() throws Exception {
        var resource = getClass().getResource("/amboditsiry/route-amboditsiry.geojson");
        assertNotNull(resource);

        var file = new File(resource.toURI());

        var result = subject.continueGeojson(file);

        assertNotNull(result);
    }
}
