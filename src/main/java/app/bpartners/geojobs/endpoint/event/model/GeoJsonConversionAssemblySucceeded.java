package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import lombok.*;

import java.time.Duration;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class GeoJsonConversionAssemblySucceeded extends PojaEvent{
    private GeoJsonConversionJob geoJsonConversionJob;

    @Override
    public Duration maxConsumerDuration() {
        return Duration.ofMinutes(1L);
    }

    @Override
    public Duration maxConsumerBackoffBetweenRetries() {
        return Duration.ofMinutes(1L);
    }
}
