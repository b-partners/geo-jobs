package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageDownloaderTest {
  private static final byte[] IMAGE_BYTES = {1, 2, 3, 4, 5};
  private static final Duration TIMEOUT = Duration.ofMillis(500);

  private final ImageDownloader subject = new ImageDownloader(TIMEOUT);
  private HttpServer server;
  private CountDownLatch serverStopped;

  @TempDir Path tempDir;

  @BeforeEach
  @SneakyThrows
  void startServer() {
    serverStopped = new CountDownLatch(1);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/image.jpg",
        exchange -> {
          exchange.sendResponseHeaders(200, IMAGE_BYTES.length);
          exchange.getResponseBody().write(IMAGE_BYTES);
          exchange.close();
        });
    server.createContext(
        "/stalled.jpg",
        exchange -> {
          awaitQuietly(serverStopped);
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
    serverStopped.countDown();
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
  void download_gives_up_when_the_server_stalls() {
    assertThrows(HttpTimeoutException.class, () -> subject.download(url("/stalled.jpg")));
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

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
