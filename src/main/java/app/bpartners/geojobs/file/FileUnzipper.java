package app.bpartners.geojobs.file;

import app.bpartners.geojobs.model.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileUnzipper implements BiFunction<ZipFile, String, Path> {
  private final FileWriter fileWriter;

  @Override
  public Path apply(ZipFile zipFile, String mainDir) {
    try {
      Path extractDirectoryPath = Files.createTempDirectory(mainDir);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory()) {
          try (InputStream is = zipFile.getInputStream(entry)) {
            String entryParentPath = getFolderPath(entry);
            String entryFilename = getFilename(entry);
            String extensionlessEntryFilename = stripExtension(entryFilename);
            Path destinationPath = extractDirectoryPath.resolve(entryParentPath);
            byte[] bytes = is.readAllBytes();
            fileWriter.write(bytes, destinationPath.toFile(), extensionlessEntryFilename);
          }
        }
      }

      return extractDirectoryPath;
    } catch (IOException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private static String getFolderPath(ZipEntry zipEntry) {
    String entryPath = zipEntry.getName();
    Path normalizedPath = Paths.get(entryPath).normalize();

    if (normalizedPath.startsWith("..")) {
      throw new IllegalArgumentException("Path traversal attempt detected");
    }

    Path parentPath = normalizedPath.getParent();
    return (parentPath != null) ? parentPath.toString() : "";
  }

  private static String getFilename(ZipEntry zipEntry) {
    String entryPath = zipEntry.getName();
    return Paths.get(entryPath).getFileName().toString();
  }

  public static String stripExtension(String filename) {
    return filename.substring(0, filename.lastIndexOf("."));
  }
}
