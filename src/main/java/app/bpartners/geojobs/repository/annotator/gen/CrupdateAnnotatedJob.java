package app.bpartners.geojobs.repository.annotator.gen;

import java.util.List;
import lombok.*;

@Builder
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CrupdateAnnotatedJob {
  private String id;
  private String name;
  private String bucketName;
  private String folderPath;
  private String ownerEmail;
  private JobStatus status;
  private String teamId;
  private List<Label> labels;
  private List<AnnotatedTask> annotatedTasks;
}
