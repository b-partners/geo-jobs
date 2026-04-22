package app.bpartners.geojobs.utils.lidar;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static java.nio.file.Files.readAllBytes;
import static java.util.Objects.requireNonNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Geometry;

public class LasRoofsPointsExtractorCreator {
  private static final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();
  private static final FileWriter fileWriter =
      new FileWriter(new ObjectMapper(), new ExtensionGuesser());

  private static SwissBoundaryChecker swissBoundaryCheckerMock() {
    var checker = mock(SwissBoundaryChecker.class);
    when(checker.isGeometryInSwiss(any())).thenReturn(false);

    return checker;
  }

  public static LasRoofsPointsExtractor create(Geometry delimitation, List<String> files) {
    var projected = projector.project(delimitation, WGS84, LAMBERT_93);
    var filesData =
        files.stream().map(LasRoofsPointsExtractorCreator::createTempFileFromResources).toList();
    var lidarApiMock = mock(LidarApiFacade.class);

    Map<String, Set<Geometry>> data = new HashMap<>();
    files.forEach(file -> data.put(file, Set.of(projected)));

    when(lidarApiMock.getUniqueLidarFilesUrls(any())).thenReturn(data);
    when(lidarApiMock.download(any()))
        .thenAnswer(
            invocation -> {
              var filename = invocation.getArguments()[0].toString();
              return Optional.of(filesData.get(files.indexOf(filename)));
            });

    return new LasRoofsPointsExtractor(lidarApiMock, projector, swissBoundaryCheckerMock());
  }

  @SneakyThrows
  public static File createTempFileFromResources(String path) {
    var lasFileFromResource =
        new File(
            requireNonNull(LasRoofsPointsExtractorCreator.class.getClassLoader().getResource(path))
                .getFile());
    return fileWriter.write(
        readAllBytes(lasFileFromResource.toPath()),
        createTempDirectory(),
        lasFileFromResource.getName());
  }
}
