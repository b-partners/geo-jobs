package app.bpartners.geojobs.service.lidar.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class CacherApi {
  private static final String CACHER_API_URL =
      "https://rsvgrlgn65cpjwdi4by3ewfxyu0sqiaq.lambda-url.eu-west-3.on.aws/cached-url";
  private final RestTemplate restTemplate;
  private final String cacherApiKey;

  public CacherApi(@Value("${cacher.api.key}") String apiKey) {
    this.cacherApiKey = apiKey;
    this.restTemplate = new RestTemplate();
  }

  public Optional<File> cache(String url) {
    String cacherUrl =
        UriComponentsBuilder.fromHttpUrl(CACHER_API_URL)
            .queryParam("encodedUrl", url)
            .queryParam("apiKey", cacherApiKey)
            .encode()
            .toUriString();

    log.info("Calling Cacher API : {}", cacherUrl);

    String presignedUrl = restTemplate.getForObject(cacherUrl, String.class);

    if (presignedUrl == null || presignedUrl.isBlank()) {
      log.warn("Cacher API returned an empty presigned URL");
      return Optional.empty();
    }

    log.info("Received presigned URL from Cacher API");

    String filename = generateFilename(url);
    return Optional.ofNullable(get(filename, URI.create(presignedUrl)));
  }

  @SneakyThrows
  public File get(String filename, URI uri) {
    log.info("Downloading {} from {}", filename, uri);
    byte[] response = restTemplate.getForObject(uri, byte[].class);
    if (response == null || response.length == 0) {
      throw new IllegalStateException("Downloaded file is empty");
    }
    return createFileFrom(filename, response);
  }

  private String generateFilename(String url) {
    return sha256(url) + getFileExtension(url);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte b : hash) {
        result.append(String.format("%02x", b));
      }
      return result.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to generate URL hash", e);
    }
  }

  private String getFileExtension(String url) {
    String cleanUrl = url.split("\\?")[0];
    int lastDot = cleanUrl.lastIndexOf('.');
    if (lastDot == -1) {
      return ".tmp";
    }
    return cleanUrl.substring(lastDot);
  }

  private static File createFileFrom(String filename, byte[] response) throws IOException {
    if (filename == null
        || filename.isBlank()
        || filename.contains("..")
        || filename.contains("/")
        || filename.contains("\\")) {
      throw new IllegalArgumentException("Invalid filename");
    }

    File file =
        Files.createTempFile(
                filename,
                null,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            .toFile();

    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      StreamUtils.copy(response, outputStream);
    }
    return file;
  }
}
