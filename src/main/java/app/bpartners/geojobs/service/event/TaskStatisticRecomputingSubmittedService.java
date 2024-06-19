package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.service.TaskStatisticFunction;
import app.bpartners.geojobs.job.service.TaskStatisticsComputing;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.TaskStatisticMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class TaskStatisticRecomputingSubmittedService
    implements Consumer<TaskStatisticRecomputingSubmitted> {
  private final TaskStatisticFunction<TilingTask, ZoneTilingJob> tilingJobTaskStatisticFunction;
  private final TaskStatisticFunction<ParcelDetectionTask, ZoneDetectionJob>
      detectionJobTaskStatisticFunction;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobRepository detectionJobRepository;
  private final TaskStatisticMailer<ZoneDetectionJob> detectionStatisticMailer;
  private final TaskStatisticMailer<ZoneTilingJob> tilingStatisticMailer;

  public TaskStatisticRecomputingSubmittedService(
      TilingTaskRepository tilingTaskRepository,
      ParcelDetectionTaskRepository parcelDetectionTaskRepository,
      ZoneTilingJobRepository tilingJobRepository,
      ZoneDetectionJobRepository detectionJobRepository,
      Mailer mailer,
      HTMLTemplateParser htmlTemplateParser) {
    this.tilingJobRepository = tilingJobRepository;
    this.detectionJobRepository = detectionJobRepository;
    this.tilingJobTaskStatisticFunction =
        new TaskStatisticFunction<>(tilingTaskRepository, new TaskStatisticsComputing<>());
    this.detectionJobTaskStatisticFunction =
        new TaskStatisticFunction<>(parcelDetectionTaskRepository, new TaskStatisticsComputing<>());
    this.detectionStatisticMailer = new TaskStatisticMailer<>(mailer, htmlTemplateParser);
    this.tilingStatisticMailer = new TaskStatisticMailer<>(mailer, htmlTemplateParser);
  }

  @Override
  public void accept(TaskStatisticRecomputingSubmitted taskStatisticRecomputingSubmitted) {
    String jobId = taskStatisticRecomputingSubmitted.getJobId();
    TaskStatistic taskStatistic;
    var optionalZTJ = tilingJobRepository.findById(jobId);
    if (optionalZTJ.isEmpty()) {
      var optionalZDJ = detectionJobRepository.findById(jobId);
      if (optionalZDJ.isEmpty()) {
        throw new NotFoundException("Neither job.id=" + jobId + " is found as ZTJ or ZDJ");
      }
      var job = optionalZDJ.get();
      taskStatistic = detectionJobTaskStatisticFunction.apply(job);
      detectionStatisticMailer.accept(taskStatistic, job);
    } else {
      var job = optionalZTJ.get();
      taskStatistic = tilingJobTaskStatisticFunction.apply(job);
      tilingStatisticMailer.accept(taskStatistic, job);
    }
  }
}
