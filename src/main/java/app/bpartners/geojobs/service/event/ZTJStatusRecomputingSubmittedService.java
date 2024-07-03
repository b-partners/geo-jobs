package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class ZTJStatusRecomputingSubmittedService
    implements Consumer<ZTJStatusRecomputingSubmitted> {
  private static final int ATTEMPT_FOR_256_MINUTES_DURATION = 8;
  private final ZoneTilingJobService zoneTilingJobService;
  private final EventProducer eventProducer;

  @Override
  public void accept(ZTJStatusRecomputingSubmitted ztjStatusRecomputingSubmitted) {
    var jobId = ztjStatusRecomputingSubmitted.getJobId();
    var lastDurationValue =
        ztjStatusRecomputingSubmitted.getMaxConsumerBackoffBetweenRetriesDurationValue();
    var attemptNb = ztjStatusRecomputingSubmitted.getAttemptNb();
    var oldJob = zoneTilingJobService.findById(jobId);
    var newJob = zoneTilingJobService.recomputeStatus(oldJob);

    if (!(newJob.isFailed() || newJob.isSucceeded())) {
      if (attemptNb < ATTEMPT_FOR_256_MINUTES_DURATION) {
        eventProducer.accept(
            List.of(
                ZTJStatusRecomputingSubmitted.builder()
                    .jobId(newJob.getId())
                    .maxConsumerBackoffBetweenRetriesDurationValue(lastDurationValue * 2)
                    .attemptNb(attemptNb + 1)
                    .build()));
      } else {
        log.error("Max attempt reached for ZTJStatusRecomputingSubmitted handler");
      }
    }
  }
}
