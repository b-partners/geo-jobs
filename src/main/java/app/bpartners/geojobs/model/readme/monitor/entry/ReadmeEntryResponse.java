package app.bpartners.geojobs.model.readme.monitor.entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeEntryResponse(
    Integer status,
    String statusText,
    List<ReadmeEntryHeader> headers,
    ReadmeEntryResponseContent content
) {}