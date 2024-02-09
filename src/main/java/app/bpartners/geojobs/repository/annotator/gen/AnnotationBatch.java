package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class AnnotationBatch {
    private String id;
    private List<Annotation> annotations;
    private Instant creationDatetime;
}
