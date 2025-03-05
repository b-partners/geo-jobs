package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.AreaPictureRepository;
import app.bpartners.geojobs.repository.model.AreaPicture;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AreaPictureService {
  private final AreaPictureRepository repository;

  public AreaPicture crupdate(AreaPicture toCreate){
    return repository.save(toCreate);
  }
}
