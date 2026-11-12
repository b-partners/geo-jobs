package app.bpartners.geojobs.service.area.mutation;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Set;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class ImageDownloader {
  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

  private final HttpClient client;
  private final Duration timeout;

  public ImageDownloader() {
    this(Duration.ofSeconds(30));
  }

  ImageDownloader(Duration timeout) {
    this.timeout = timeout;
    this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  @SneakyThrows
  public File download(String imageUrl) {
    var uri = URI.create(imageUrl);
    if (uri.getScheme() == null || !ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase())) {
      throw new IllegalArgumentException("Only http(s) image urls are supported: " + imageUrl);
    }
    var destination =
        File.createTempFile("area_picture_" + randomUUID(), ".jpg", createTempDirectory());
    var request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
    var response = client.send(request, BodyHandlers.ofFile(destination.toPath()));
    if (response.statusCode() / 100 != 2) {
      throw new IOException(
          "Image download failed with status " + response.statusCode() + ": " + imageUrl);
    }
    return destination;
  }
}
