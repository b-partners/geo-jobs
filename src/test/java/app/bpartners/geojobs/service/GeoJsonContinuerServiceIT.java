package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.GeoJsonContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonContinuation;
import app.bpartners.geojobs.service.geojson.GeoJsonContinuerService;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
public class GeoJsonContinuerServiceIT extends FacadeIT {

  @Autowired private GeoJsonContinuerService subject;
  @MockBean private EventProducer eventProducer;
  @MockBean private GeoJsonContinuationRepository repository;
  @MockBean private BucketComponent bucketComponent;

  @Test
  void read_geo_json_url_string() throws MalformedURLException {
    var file = getClass().getResource("/amboditsiry/route-amboditsiry.geojson");
    var id = "amboditsiry1";
    var geoJsonContinuationSubjet = new GeoJsonContinuation();
    geoJsonContinuationSubjet.setId(id);
    geoJsonContinuationSubjet.setFileKey("continuations/geojson/" + id + ".geojson");

    when(repository.findById(id))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(geoJsonContinuationSubjet));
    when(bucketComponent.upload(any(File.class), anyString())).thenReturn(mock(FileHash.class));
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(
            URI.create("https://aws-s3.com/continuations/geojson/amboditsiry1.geojson").toURL());

    String actual = subject.generatePresignedUrl(id, new File(file.getFile()));
    verify(bucketComponent).presign(anyString(), any(Duration.class));

    assertNotNull(actual);
    assertNotNull(file);
    assertEquals("https://aws-s3.com/continuations/geojson/amboditsiry1.geojson", actual);
  }
}
