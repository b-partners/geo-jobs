package app.bpartners.geojobs.endpoint.rest.security.readme.monitor.model.entry;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.io.Serializable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeEntryRequest(
    String method,
    String url,
    String httpVersion,
    List<ReadmeEntryHeader> headers,
    List<ReadmeEntryQuery> queryString
) implements Serializable {}
