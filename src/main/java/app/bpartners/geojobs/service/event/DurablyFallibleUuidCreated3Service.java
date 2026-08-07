package app.bpartners.geojobs.service.event;

import static java.lang.Thread.sleep;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.endpoint.event.model.DurablyFallibleUuidCreated3;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@PojaGenerated
@SuppressWarnings("all")
@Service
@AllArgsConstructor
@Slf4j
public class DurablyFallibleUuidCreated3Service implements Consumer<DurablyFallibleUuidCreated3> {
  private final UuidCreatedService uuidCreatedService;

  @SneakyThrows
  @Override
  public void accept(DurablyFallibleUuidCreated3 durablyFallibleUuidCreated3) {
    sleep(durablyFallibleUuidCreated3.getWaitDurationBeforeConsumingInSeconds() * 1_000L);
    if (durablyFallibleUuidCreated3.shouldFail()) {
      throw new RuntimeException("Oops, random fail!");
    }

    uuidCreatedService.accept(durablyFallibleUuidCreated3.getUuidCreated());
  }
}
