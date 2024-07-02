package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.file.FileZipper.ZIP_FILE_SUFFIX;
import static org.junit.Assert.assertThrows;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.FileUnzipper;
import app.bpartners.geojobs.file.FileWriter;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FileUnzipperTest extends FacadeIT {
  @Mock FileUnzipper fileUnzipper;
  @Mock FileWriter fileWriter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    fileUnzipper = new FileUnzipper(fileWriter);
  }

  ZipFile zipFileIllegalArgumentException() throws IOException {
    var zipFile = File.createTempFile("maliciousZipFile", ZIP_FILE_SUFFIX);
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      zos.putNextEntry(new ZipEntry(".."));
      zos.finish();
      zos.flush();
      return new ZipFile(zipFile);
    }
  }

  void applyFileUnzipperIllegalArgumentException() throws IOException {
    fileUnzipper.apply(zipFileIllegalArgumentException(), "/mainDir");
  }

  @Test
  void getFolderPathIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> applyFileUnzipperIllegalArgumentException());
  }
}
