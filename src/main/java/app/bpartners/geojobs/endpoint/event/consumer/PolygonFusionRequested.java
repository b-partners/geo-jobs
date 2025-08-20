package app.bpartners.geojobs.endpoint.event.consumer;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.time.Duration;

/**
 * Événement indiquant que l'union de polygones a été demandé,
 * contenant en particulier le fichier concerné.
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

