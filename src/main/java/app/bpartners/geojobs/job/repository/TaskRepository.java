package app.bpartners.geojobs.job.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface TaskRepository<T> extends JpaRepository<T, String> {
  List<T> findAllByJobId(String id);

  Optional<T> findByAsJobId(String asJobId);
}
