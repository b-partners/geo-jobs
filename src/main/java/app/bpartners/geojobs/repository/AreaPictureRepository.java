package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.AreaPicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaPictureRepository extends JpaRepository<AreaPicture, String> {}
