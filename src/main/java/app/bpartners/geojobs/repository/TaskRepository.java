package app.bpartners.geojobs.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface TaskRepository<T> extends JpaRepository<T, String> {
  List<T> findAllByJobId(String id);
}
