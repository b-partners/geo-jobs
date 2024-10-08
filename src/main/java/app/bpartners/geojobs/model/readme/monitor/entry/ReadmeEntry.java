package app.bpartners.geojobs.model.readme.monitor.entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import java.io.Serializable;
import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeEntry(
    @JsonProperty("pageref") String pareRef,
    Long time,
    Instant startedDateTime,
    ReadmeEntryRequest request,
    ReadmeEntryResponse response
) implements Serializable {}