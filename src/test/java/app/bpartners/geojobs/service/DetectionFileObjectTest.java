package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.model.DetectionFileObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DetectionFileObjectTest {
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @SneakyThrows
  @Test
  void download_file_objects() {
    var fileObjectFile = new ClassPathResource("/fileObjects/file_object_example.json").getFile();

    var actual =
        objectMapper.readValue(fileObjectFile, new TypeReference<List<DetectionFileObject>>() {});

    assertNotNull(actual);
  }
}
