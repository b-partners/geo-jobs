package app.bpartners.geojobs.file;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileWriter implements BiFunction<byte[], File, File> {
  private static final String TEMP_FOLDER_PERMISSION = "rwx------";
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

  public File combineContent(List<File> files, String outputFileName) {
    var outputFile = createTempDirectory();
    try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(outputFile))) {
      for (File file : files) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
          String line;
          while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
          }
        } catch (IOException e) {
          throw new ApiException(
              SERVER_EXCEPTION,
              "Exception while reading file content for File.name=" + file.getName());
        }
      }
    } catch (IOException e) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "Exception while write file content for File.name=" + outputFile.getName());
    }
    return outputFile;
  }

  @SneakyThrows
  public static File createTempDirectory() {
    Path tempDir =
        Files.createTempDirectory(
            randomUUID().toString(), asFileAttribute(fromString(TEMP_FOLDER_PERMISSION)));
    var dirFile = tempDir.toFile();
    dirFile.deleteOnExit();
    return dirFile;
  }
}
