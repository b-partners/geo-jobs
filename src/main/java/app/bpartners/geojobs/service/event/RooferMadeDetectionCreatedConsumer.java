package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.lang.Runtime.getRuntime;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.model.RooferMadeDetectionCreated;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RooferMadeDetectionCreatedConsumer implements Consumer<RooferMadeDetectionCreated> {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;
  private final TileObjectDetector detector;
  private final ExecutorService executorService =
      newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() - 1));

  @Override
  public void accept(RooferMadeDetectionCreated event) {
    var detectionConf =
        detectionRepository
            .findByZdjId(event.getZdjId())
            .orElseThrow()
            .getDetectableObjectConfigurations();
    var zdj = zoneDetectionJobService.getMachineZdjFromZdjId(event.getZdjId());
    var parcels =
        zoneDetectionJobService.getTasks(zdj).stream()
            .map(ParcelDetectionTask::getParcel)
            .collect(toSet());
    var detectionTasks = mapToDetectionTask(parcels);
    try {
      executorService.invokeAll(
          detectionTasks.stream()
              .map(
                  task ->
                      ((Callable<TileDetectionTask>)
                          () -> {
                            detector.apply(task, detectionConf);
                            return task;
                          }))
              .collect(toSet()));
    } catch (InterruptedException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  private Set<TileDetectionTask> mapToDetectionTask(Set<Parcel> parcels) {
    return parcels.stream()
        .map(
            parcel -> {
              var parcelId = parcel.getId();
              var tiles = parcel.getParcelContent().getTiles();
              return tiles.stream()
                  .map(
                      tile ->
                          TileDetectionTask.builder()
                              .id(randomUUID().toString())
                              .parcelId(parcelId)
                              .tile(tile)
                              .build());
            })
        .flatMap(Stream::distinct)
        .collect(toSet());
  }
}
