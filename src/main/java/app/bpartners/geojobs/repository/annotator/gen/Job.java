package app.bpartners.geojobs.repository.annotator.gen;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
@JsonAutoDetect(fieldVisibility = ANY)
public class Job {
  private String id;
  private JobType type;
  private String name;
  private String bucketName;
  private String folderPath;
  private String ownerEmail;
  private JobStatus status;
  private String teamId;
  private List<Label> labels;
  private TaskStatistics taskStatistics;
}
