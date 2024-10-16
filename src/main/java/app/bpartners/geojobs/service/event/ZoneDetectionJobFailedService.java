package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneDetectionJobFailedService implements Consumer<ZoneDetectionJobFailed> {

  @Override
  public void accept(ZoneDetectionJobFailed event) {
    throw new NotImplementedException(
        "Failed ZDJ(id=" + event.getFailedJobId() + "not supported for now");
  }
}
