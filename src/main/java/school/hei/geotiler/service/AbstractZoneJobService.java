package school.hei.geotiler.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import school.hei.geotiler.endpoint.event.EventProducer;
import school.hei.geotiler.model.BoundedPageSize;
import school.hei.geotiler.model.PageFromOne;
import school.hei.geotiler.model.exception.NotFoundException;
import school.hei.geotiler.repository.model.AbstractZoneJob;
import school.hei.geotiler.repository.model.Status;

// TODO: Consider refactoring for better abstraction - warrants discussion
@AllArgsConstructor
@Data
public class AbstractZoneJobService<
    S extends Status, T, J extends AbstractZoneJob<S, T>, R extends JpaRepository<J, String>> {
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

  public J updateStatus(J job, S status) {
    job.addStatus(status);
    return getRepository().save(job);
  }
}
