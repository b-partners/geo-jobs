package app.bpartners.geojobs.endpoint.rest.security.readme.monitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/*
 *   docs: https://docs.readme.com/main/reference/post_request
 * */
@Builder
@JsonInclude(NON_NULL)
public record ReadmeLog(
    @JsonProperty("_id") String id,
    String clientIPAddress,
    Boolean development,
    ReadmeGroup group,
    ReadmeRequest request
) implements Serializable {}
