package app.bpartners.geojobs.file.zip;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.model.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
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

  private static final int THRESHOLD_ENTRIES = 10000;
  private static final int THRESHOLD_SIZE = 1000000000; // 1 GB
  private static final double THRESHOLD_RATIO = 10.0;

  @Override
  public Path apply(ZipFile zipFile, String mainDir) {
    try {
      Path extractDirectoryPath = fileWriter.createSecureTempDirectory(mainDir);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      int totalEntryArchive = 0;
      int totalSizeArchive = 0;

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory()) {
          totalEntryArchive++;
          // Ensure the target file is within the intended directory
          Path targetFile = extractDirectoryPath.resolve(entry.getName()).normalize();
          if (!targetFile.startsWith(extractDirectoryPath)) {
            throw new IOException("Entry is outside of the target dir: " + entry.getName());
          }
          if (totalEntryArchive > THRESHOLD_ENTRIES) {
            throw new IOException("Too many entries in the archive, potential Zip Bomb Attack");
          }

          try (InputStream is = zipFile.getInputStream(entry)) {
            int totalSizeEntry = 0;
            byte[] buffer = new byte[2048];
            int nBytes;
            while ((nBytes = is.read(buffer)) > 0) {
              totalSizeEntry += nBytes;
              totalSizeArchive += nBytes;
              double compressionRatio = (double) totalSizeEntry / entry.getCompressedSize();

              if (compressionRatio > THRESHOLD_RATIO) {
                throw new IOException(
                    "Suspicious compression ratio detected, potential Zip Bomb Attack");
              }
              if (totalSizeArchive > THRESHOLD_SIZE) {
                throw new IOException(
                    "Total uncompressed size exceeds limit, potential Zip Bomb Attack");
              }
            }
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
