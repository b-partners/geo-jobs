package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.zip.FileZipper;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class FileZipperTest {
  FileWriter fileWriter = mock();
  FileZipper subject = new FileZipper(fileWriter);

  @Test
  @SneakyThrows
  void zip_file_ok() {
    var files =
        List.of(
            File.createTempFile("randomFile1", ".txt"), File.createTempFile("randomFile2", ".txt"));

    when(fileWriter.createTempFileSecurely(any(), any())).thenReturn(new File("").toPath());

    var actual = subject.apply(files);
    var zipEntryList = enumerationToList(actual.entries());
    assertEquals(files.size(), zipEntryList.size());
    actual.close();
  }

  private List<ZipEntry> enumerationToList(Enumeration<? extends ZipEntry> zipEntries) {
    List<ZipEntry> list = new ArrayList<>();
    while (zipEntries.hasMoreElements()) {
      list.add(zipEntries.nextElement());
    }
    return list;
  }
}
