package app.bpartners.geojobs.endpoint.rest.security.readme.monitor.model;

import app.bpartners.geojobs.endpoint.rest.security.readme.monitor.model.entry.ReadmeEntry;
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
        ReadmeRequestCreator creator,
        List<ReadmeEntry> entries
    ) implements Serializable {}
}
