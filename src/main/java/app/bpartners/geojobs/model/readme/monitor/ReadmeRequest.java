package app.bpartners.geojobs.model.readme.monitor;

import app.bpartners.geojobs.model.readme.monitor.entry.ReadmeEntry;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeRequest(
    ReadmeRequestLog log
) implements Serializable {

    @Builder
    @JsonInclude(NON_NULL)
    public record ReadmeRequestLog(
        List<ReadmeEntry> entries
    ) implements Serializable {}
}
