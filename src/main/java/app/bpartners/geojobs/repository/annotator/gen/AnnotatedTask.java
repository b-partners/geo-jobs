package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class AnnotatedTask {
    private String id;
    private String annotatorId;
    private String filename;
    private AnnotationBatch annotationBatch;
}
