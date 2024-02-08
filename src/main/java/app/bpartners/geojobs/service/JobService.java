package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.Job;
import app.bpartners.geojobs.repository.model.Task;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@AllArgsConstructor
public class JobService<T extends Task, J extends Job<T>> {
  protected final JpaRepository<J, String> repository;
  protected final EventProducer eventProducer;

  public List<J> findAll(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return repository.findAll(pageable).toList();
  }

  public J findById(String id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("job.id=" + id));
  }

  public J refreshStatus(J oldJob) {
    var newJob = repository.findById(oldJob.getId()).orElseThrow();
    newJob.refreshStatusHistory();
    repository.save(newJob);

    var oldStatus = oldJob.getStatus();
    var newStatus = newJob.getStatus();
    if (!oldStatus.equals(newStatus)) {
      onStatusChanged(oldJob, newJob);
    }
    return newJob;
  }

  protected void onStatusChanged(J oldJob, J newJob) {}

  public J create(J job) {
    if (!job.isPending()) {
      throw new IllegalArgumentException(
          "Only PENDING job can be created. " + "You sure all tasks are PENDING?");
    }

    return repository.save(job);
  }
}
