package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionJobStatusChanged;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ParcelDetectionJobStatusChangedService
    implements Consumer<ParcelDetectionJobStatusChanged> {
  private final StatusChangedHandler statusChangedHandler;
  private final ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  private final TaskStatusService<ParcelDetectionTask> taskStatusService;

  @Override
  public void accept(ParcelDetectionJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    statusChangedHandler.handle(
        event,
        newJob.getStatus(),
        oldJob.getStatus(),
        new OnSucceededHandler(newJob, parcelDetectionTaskRepository, taskStatusService),
        new OnFailedHandler(newJob, parcelDetectionTaskRepository, taskStatusService));
  }

  private record OnSucceededHandler(
      ParcelDetectionJob newJob,
      ParcelDetectionTaskRepository parcelDetectionTaskRepository,
      TaskStatusService<ParcelDetectionTask> taskStatusService)
      implements StatusHandler {
    @Override
    @Transactional
    public String performAction() {
      var taskFromJob =
          parcelDetectionTaskRepository
              .findByAsJobId(newJob.getId())
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Unable to found task associated to ParcelDetectionJob(id"
                              + newJob.getId()
                              + ")"));
      taskStatusService.succeed(taskFromJob);
      return "ParcelDetectionTask(id=" + taskFromJob.getId() + ") successfully finished";
    }
  }

  private record OnFailedHandler(
      ParcelDetectionJob newJob,
      ParcelDetectionTaskRepository parcelDetectionTaskRepository,
      TaskStatusService<ParcelDetectionTask> taskStatusService)
      implements StatusHandler {
    @Override
    @Transactional
    public String performAction() {
      var taskFromJob =
          parcelDetectionTaskRepository
              .findByAsJobId(newJob.getId())
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Unable to found task associated to ParcelDetectionJob(id"
                              + newJob.getId()
                              + ")"));
      taskStatusService.fail(taskFromJob);
      return "Failed to process ParcelDetectionTask(id=" + taskFromJob.getId() + ")";
    }
  }
}
