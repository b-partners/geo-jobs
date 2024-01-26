package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.ZoneJob;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// TODO: Consider refactoring for better abstraction - warrants discussion
@AllArgsConstructor
@Data
public class AbstractZoneJobService<T, J extends ZoneJob<T>, R extends JpaRepository<J, String>> {
  private final EventProducer eventProducer;
  private final R repository;

  public List<J> findAll(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return repository.findAll(pageable).toList();
  }

  public J findById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("ZoneJob.Id " + id + " not found"));
  }

  public J updateStatus(J job, JobStatus status) {
    job.addStatus(status);
    return getRepository().save(job);
  }
}
