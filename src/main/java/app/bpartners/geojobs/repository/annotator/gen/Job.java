package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode
@ToString
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
