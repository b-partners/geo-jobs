package app.bpartners.geojobs.service.lidar.geometry;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.api.LidarApi;
import app.bpartners.geojobs.service.lidar.model.geometry.InclinedPlaneExtractor;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class InclinedPlaneExtractorIT extends FacadeIT {
  private static final InclinedPlaneExtractor subject = new InclinedPlaneExtractor();

  @Autowired LidarRoofsAnalysisProcessor processor;
  @MockBean LidarApi lidarApiMock;
  @Autowired FileWriter fileWriter;
  @Autowired GeometrySquareMeterArea projector;

  File lasFile;

  @BeforeEach
  void setUp() {
    lasFile = createLidarTestTempFile();
  }

  @AfterEach
  @SneakyThrows
  void cleanUp() {
    Files.deleteIfExists(lasFile.toPath());
  }

  @Test
  void should_separate_inclined_planes_from_lidar() {
    var roofGeometry = roofGeometryWith2Planes();
    var projected = projector.project(roofGeometry, WGS84, LAMBERT_93);

    when(lidarApiMock.getUniqueLidarFilesUrls(Set.of(projected)))
        .thenReturn(Map.of("url", Set.of(projected)));
    when(lidarApiMock.download(any())).thenReturn(Optional.of(lasFile));

    var result = processor.apply(Set.of(roofGeometry));
    var property = result.getProperties(roofGeometry);

    var planes = subject.apply(property.cleanedRoofData());
    assertEquals(2, planes.size());
  }

  private static Geometry roofGeometryWith2Planes() {
    var roofGeometry =
        new Coordinate[] {
          new Coordinate(2.243822051637153, 48.82470351681039),
          new Coordinate(2.2438825948823933, 48.824583284573066),
          new Coordinate(2.2440969775185238, 48.824634906058066),
          new Coordinate(2.2440423893471007, 48.82475317786745),
          new Coordinate(2.243822051637153, 48.82470351681039)
        };
    return geometryFactory.createPolygon(roofGeometry);
  }

  @SneakyThrows
  private File createLidarTestTempFile() {
    var lasFileFromResource =
        new File(
            requireNonNull(
                    getClass()
                        .getClassLoader()
                        .getResource("las/LHD_FXX_0644_6859_PTS_O_LAMB93_IGN69.copc.laz"))
                .getFile());

    return fileWriter.write(
        Files.readAllBytes(lasFileFromResource.toPath()),
        FileWriter.createTempDirectory(),
        lasFileFromResource.getName());
  }
}
