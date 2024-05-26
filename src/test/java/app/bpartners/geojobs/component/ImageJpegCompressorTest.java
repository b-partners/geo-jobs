package app.bpartners.geojobs.component;

import static app.bpartners.geojobs.service.detection.HttpApiTileObjectDetector.IMAGE_QUALITY;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.file.ImageJpegCompressor;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public class ImageJpegCompressorTest {
  ImageJpegCompressor subject = new ImageJpegCompressor();

  @Test
  void compress_image_ok() throws IOException {
    ClassPathResource resource1 = new ClassPathResource("images/tile-1.jpg");
    ClassPathResource resource2 = new ClassPathResource("images/tile-2.jpg");
    File file1 = resource1.getFile();
    File file2 = resource2.getFile();
    float quality = IMAGE_QUALITY;

    File actual1 = subject.apply(file1, quality);
    File actual2 = subject.apply(file2, quality);

    long actual1Length = actual1.length();
    long initial1Length = file1.length();
    long actual2Length = actual2.length();
    long initial2Length = file2.length();
    log.info("Actual 1 size {}, initial 1 size {}", actual1Length, initial1Length);
    log.info("Actual 2 size {}, initial 2 size {}", actual2Length, initial2Length);
    assertTrue(actual1Length <= initial1Length * quality);
    assertTrue(actual2Length <= initial2Length * quality);
  }
}
