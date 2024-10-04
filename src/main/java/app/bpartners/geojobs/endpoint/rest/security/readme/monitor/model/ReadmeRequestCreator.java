package app.bpartners.geojobs.endpoint.rest.security.readme.monitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ReadmeRequestCreator(
    String name,
    String version,
    String comment
) implements Serializable {}
