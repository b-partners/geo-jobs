package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.InDoubtTileDetected;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InDoubtTileDetectedService implements Consumer<InDoubtTileDetected> {
  @Override
  public void accept(InDoubtTileDetected inDoubtTileDetected) {
    throw new NotImplementedException("Not supported : " + inDoubtTileDetected);
  }
}
