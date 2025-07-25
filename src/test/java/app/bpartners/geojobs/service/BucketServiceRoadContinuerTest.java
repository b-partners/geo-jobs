package app.bpartners.geojobs.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BucketServiceRoadContinuerTest {

  @Mock private BucketComponent bucketComponent;

  @InjectMocks private BucketServiceRoadContinuer service;

  @Test
  void shouldReturnPresignedUrlMap() throws IOException {
    File dummyFile = File.createTempFile("test", ".txt");
    String adminApiKey = "admin-key";
    String expectedUrl = "https://example.com/presigned-url";

    when(bucketComponent.presign(adminApiKey, Duration.ofHours(1)))
        .thenReturn(URI.create(expectedUrl).toURL());
    Map<String, String> actual = service.getContinuedRoutePresignedUrl(dummyFile, adminApiKey);

    verify(bucketComponent).upload(dummyFile, adminApiKey);
    verify(bucketComponent).presign(adminApiKey, Duration.ofHours(1));
    assertThat(actual).containsKey("url").containsValue(expectedUrl);
  }
}
