package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageDownloaderTest {
  private static final byte[] IMAGE_BYTES = {1, 2, 3, 4, 5};

  private final ImageDownloader subject = new ImageDownloader();
  private HttpServer server;

  @TempDir Path tempDir;

  @BeforeEach
  @SneakyThrows
  void startServer() {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/image.jpg",
        exchange -> {
          exchange.sendResponseHeaders(200, IMAGE_BYTES.length);
          exchange.getResponseBody().write(IMAGE_BYTES);
          exchange.close();
        });
    server.createContext(
        "/missing.jpg",
        exchange -> {
          exchange.sendResponseHeaders(404, -1);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  @SneakyThrows
  void download_writes_the_source_bytes_to_a_local_file() {
    var downloaded = subject.download(url("/image.jpg"));

    assertTrue(downloaded.exists());
    assertArrayEquals(IMAGE_BYTES, Files.readAllBytes(downloaded.toPath()));
  }

  @Test
  void download_fails_on_a_non_2xx_response() {
    assertThrows(IOException.class, () -> subject.download(url("/missing.jpg")));
  }

  @Test
  @SneakyThrows
  void download_refuses_non_http_urls() {
    var localFile = tempDir.resolve("source.jpg");
    Files.write(localFile, IMAGE_BYTES);
    var fileUrl = localFile.toUri().toString();

    assertThrows(IllegalArgumentException.class, () -> subject.download(fileUrl));
  }

  private String url(String path) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + path;
  }
}
