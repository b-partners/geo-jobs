package app.bpartners.geojobs.service.tiling;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.TilingTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneTilingJobService extends JobService<TilingTask, ZoneTilingJob> {

  public ZoneTilingJobService(
      JpaRepository<ZoneTilingJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskRepository<TilingTask> taskRepository,
      EventProducer eventProducer) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, ZoneTilingJob.class);
  }

  @Transactional
  public ZoneTilingJob duplicate(String jobId) {
    var optionalZoneTilingJob = repository.findById(jobId);
    if (optionalZoneTilingJob.isEmpty()) {
      throw new BadRequestException("ZoneTilingJob(id=" + jobId + ") not found");
    }
    var job = optionalZoneTilingJob.get();
    var duplicatedJobId = randomUUID().toString();
    var tilingTasks = taskRepository.findAllByJobId(jobId);
    var duplicatedTasks =
        tilingTasks.stream()
            .map(
                task -> {
                  var newTaskId = randomUUID().toString();
                  var newParcelId = randomUUID().toString();
                  var newParcelContentId = randomUUID().toString();
                  return task.duplicate(
                      newTaskId, duplicatedJobId, newParcelId, newParcelContentId);
                })
            .toList();
    ZoneTilingJob duplicatedJob = repository.save(job.duplicate(duplicatedJobId));
    taskRepository.saveAll(duplicatedTasks);
    return duplicatedJob;
  }

  @Transactional
  @Override
  public ZoneTilingJob create(ZoneTilingJob job, List<TilingTask> tasks) {
    var saved = super.create(job, tasks);
    eventProducer.accept(List.of(new ZoneTilingJobCreated(saved)));
    return saved;
  }

  @Transactional
  public void fireTasks(ZoneTilingJob job) {
    getTasks(job).forEach(task -> eventProducer.accept(List.of(new TilingTaskCreated(task))));
  }

  @Override
  protected void onStatusChanged(ZoneTilingJob oldJob, ZoneTilingJob newJob) {
    eventProducer.accept(
        List.of(ZoneTilingJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }
}
