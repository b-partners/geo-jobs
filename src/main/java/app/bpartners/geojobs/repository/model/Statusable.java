package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import java.util.List;

public interface Statusable<S extends Status> {
  List<S> getStatusHistory();

  void setStatusHistory(List<S> statusHistory);

  default S getStatus() {
    var statusHistory = getStatusHistory();
    return statusHistory.isEmpty()
        ? from(
            Status.builder().progression(PENDING).health(UNKNOWN).creationDatetime(now()).build())
        : from(
            statusHistory.stream()
                .map(status -> (Status) status)
                .sorted(comparing(Status::getCreationDatetime, naturalOrder()).reversed())
                .toList()
                .get(0));
  }

  default void hasNewStatus(Status status) {
    getStatusHistory().add(from(getStatus().to(from(status))));
  }

  S from(Status status);
}
