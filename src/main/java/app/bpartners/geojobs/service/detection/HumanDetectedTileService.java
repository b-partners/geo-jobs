package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HumanDetectedTileService {
  private final HumanDetectedTileRepository repository;

  public List<HumanDetectedTile> saveAll(List<HumanDetectedTile> tiles) {
    return repository.saveAll(tiles);
  }
}
