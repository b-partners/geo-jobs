package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.file.zip.FileZipper.ZIP_FILE_SUFFIX;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.zip.FileUnzipper;
import app.bpartners.geojobs.model.exception.ApiException;
import java.io.*;
import java.util.Enumeration;
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

  @Test
  public void testEntryOutsideTargetDirThrowsIOException() throws IOException {
    var mainDir = "testDir";
    var zipFile = mock(ZipFile.class);
    var zipEntryMock = mock(ZipEntry.class);
    var entries = mock(Enumeration.class);

    when(zipFile.entries()).thenReturn(entries);
    when(entries.hasMoreElements()).thenReturn(true, false);
    when(entries.nextElement()).thenReturn(zipEntryMock);
    when(zipEntryMock.isDirectory()).thenReturn(false);
    when(zipEntryMock.getName()).thenReturn("/evilFile.txt");
    when(fileWriter.createSecureTempDirectory(any())).thenReturn(new File("").toPath());

    assertThrows(
        ApiException.class,
        () -> {
          fileUnzipper.apply(zipFile, mainDir);
        });
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
  void getFolderPathIllegalArgumentException() throws IOException {
    when(fileWriter.createSecureTempDirectory(any())).thenReturn(new File("").toPath());

    assertThrows(IllegalArgumentException.class, () -> applyFileUnzipperIllegalArgumentException());
  }

  @Test
  void testThrowsApiExceptionOnIOException() {
    var zipFile = mock(ZipFile.class);
    var entries = mock(Enumeration.class);

    when(zipFile.entries()).thenReturn(entries);
    when(entries.hasMoreElements()).thenReturn(true);
    when(entries.nextElement())
        .thenThrow(
            new ApiException(
                ApiException.ExceptionType.SERVER_EXCEPTION,
                new IOException("Simulated IO error")));

    assertThrows(ApiException.class, () -> fileUnzipper.apply(zipFile, "mainDir"));
  }
}
