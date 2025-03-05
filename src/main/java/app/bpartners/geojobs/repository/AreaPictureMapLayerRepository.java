package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.AreaPictureMapLayer;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaPictureMapLayerRepository extends JpaRepository<AreaPictureMapLayer, String> {
  List<AreaPictureMapLayer> findAreaPictureMapLayerByDepartmentNameIgnoreCase(
      Collection<String> departmentName);
}
