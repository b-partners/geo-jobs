package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class Label {
    private String id;
    private String name;
    private String color;
}
