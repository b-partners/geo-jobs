package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import lombok.*;

import java.time.Duration;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class GeoJsonContinuerIsCompleted extends PojaEvent {
    private String bucketKey;
    private String presigneURL;

    @Override
    public Duration maxConsumerDuration() {
        return Duration.ofMinutes(3L);
    }

    @Override
    public Duration maxConsumerBackoffBetweenRetries() {
        return Duration.ofMinutes(1L);
    }
}
