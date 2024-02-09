package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class Annotation {
    private String id;
    private String taskId;
    private String userId;
    private Label label;
    private Polygon polygon;
}
