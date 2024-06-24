package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ParcelDetectionJobCreatedService implements Consumer<ParcelDetectionJobCreated> {
  private final TileDetectionTaskRepository tileDetectionTaskRepository;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(ParcelDetectionJobCreated parcelDetectionJobCreated) {
    // TODO: add ParcelDetectionJobStatusComputingSubmitted event here
    var parcelDetectionJob = parcelDetectionJobCreated.getParcelDetectionJob();
    var zdjId = parcelDetectionJobCreated.getZdjId();
    var jobId = parcelDetectionJob.getId();
    var tileDetectionTasks = tileDetectionTaskRepository.findAllByJobId(jobId);
    log.info("[DEBUG] TileDetectionTasks size={} and content ={}", tileDetectionTasks.size(), tileDetectionTasks.stream().map(TileDetectionTask::describeTile));
    var detectableTypes =
        objectConfigurationRepository.findAllByDetectionJobId(zdjId).stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .toList();

    tileDetectionTasks.forEach(
        tileDetectionTask ->
            eventProducer.accept(
                List.of(new TileDetectionTaskCreated(zdjId, tileDetectionTask, detectableTypes))));
  }
}
