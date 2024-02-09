package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class Task {
    private String id;
    private String userId;
    private TaskStatus status;
    private String imageUri;
    private String filename;
}
