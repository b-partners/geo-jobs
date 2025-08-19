package app.bpartners.geojobs.endpoint.event.consumer;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.time.Duration;

/**
 * Événement déclenché à la fin d’un job de fusion de polygones, contenant les résultats.
 * Étend {@link PojaEvent} et identifie sa source comme "PolygonFusion".
 */
@AllArgsConstructor
@Getter
@ToString
public class PolygonFusionRequested extends PojaEvent {
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

