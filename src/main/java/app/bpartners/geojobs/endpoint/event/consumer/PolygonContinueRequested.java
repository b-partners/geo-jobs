package app.bpartners.geojobs.endpoint.event.consumer;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.time.Duration;

@AllArgsConstructor
@Getter
@ToString
public class PolygonContinueRequested extends PojaEvent {
    private final File file;

    @Override
    public Duration maxConsumerDuration() {
        return Duration.ofMinutes(1L);
    }

    @Override
    public Duration maxConsumerBackoffBetweenRetries() {
        return Duration.ofSeconds(2L);
    }
}

