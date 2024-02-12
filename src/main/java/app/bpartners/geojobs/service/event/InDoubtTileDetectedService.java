package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.InDoubtTilesDetected;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InDoubtTileDetectedService implements Consumer<InDoubtTilesDetected> {
  @Override
  public void accept(InDoubtTilesDetected inDoubtTilesDetected) {
    throw new NotImplementedException("Not supported : " + inDoubtTilesDetected);
  }
}
