package app.bpartners.geojobs.file;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileWriter implements BiFunction<byte[], File, File> {
  private final ObjectMapper objectMapper;
  private final ExtensionGuesser extensionGuesser;

  public byte[] writeAsByte(Object object) {
    try {
      return objectMapper.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      throw new ApiException(SERVER_EXCEPTION, "error during object conversion to bytes");
    }
  }

  @Override
  public File apply(byte[] bytes, @Nullable File directory) {
    try {
      String name = randomUUID().toString();
      String suffix = "." + extensionGuesser.apply(bytes);
      File tempFile = File.createTempFile(name, suffix, directory);
      return Files.write(tempFile.toPath(), bytes).toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public File write(byte[] bytes, @Nullable File directory, String filename) {
    if (directory != null && directory.getName().contains("..")) {
      throw new IllegalArgumentException("name must not contain .. but receceived: pathValue");
    }
    try {
      String suffix = extensionGuesser.apply(bytes);
      File newFile = new File(directory, filename + suffix);
      Files.createDirectories(newFile.toPath().getParent());
      return Files.write(newFile.toPath(), bytes).toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public Path createTempFileSecurely(String prefix, String suffix) throws IOException {
    // Create a temporary file securely, restricting permissions
    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
    Path tempFile =
        Files.createTempFile(prefix, suffix, PosixFilePermissions.asFileAttribute(permissions));

    // Ensure the directory itself is also secured
    Path parentDir = tempFile.getParent();
    Files.setPosixFilePermissions(parentDir, permissions);

    return tempFile;
  }

  public Path createSecureTempDirectory(String mainDir) throws IOException {
    // Create a temporary directory securely with restricted permissions
    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
    Path tempDirectory =
        Files.createTempDirectory(mainDir, PosixFilePermissions.asFileAttribute(permissions));

    // Ensure the directory itself is also secured
    Files.setPosixFilePermissions(tempDirectory, permissions);

    return tempDirectory;
  }
}
