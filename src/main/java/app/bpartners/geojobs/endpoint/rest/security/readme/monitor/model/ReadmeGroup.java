package app.bpartners.geojobs.endpoint.rest.security.readme.monitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeGroup(
    String id,
    String email,
    String label
) implements Serializable {}
