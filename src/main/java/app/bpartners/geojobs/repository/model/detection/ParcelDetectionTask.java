package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity(name = "parcel_detection_task_entity")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@JsonIgnoreProperties({"status"})
public class ParcelDetectionTask extends Task implements Serializable {
  @ManyToMany(cascade = ALL, fetch = EAGER)
  @JoinTable(
      name = "parcel_detection_task",
      joinColumns = @JoinColumn(name = "id_detection_task"),
      inverseJoinColumns = @JoinColumn(name = "id_parcel"))
  private List<Parcel> parcels;

  public List<Tile> getTiles() {
    return getParcel() == null ? null : getParcel().getParcelContent().getTiles();
  }

  public Parcel getParcel() {
    if (parcels == null || parcels.isEmpty()) return null;
    var chosenParcel = parcels.get(0);
    if (parcels.size() > 1) {
      log.error(
          "DetectionTask(id={}) contains multiple parcels but only one Parcel(id={}) is handle for"
              + " now",
          getId(),
          chosenParcel.getId());
    }
    return chosenParcel;
  }

  @Override
  public GeoJobType getJobType() {
    return DETECTION;
  }

  @Override
  public ParcelDetectionTask semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }

  public ParcelDetectionTask duplicate(
      String taskId, String jobId, String parcelId, String parcelContentId, String asJobId) {
    return ParcelDetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .asJobId(asJobId)
        .parcels(
            parcels == null
                ? null
                : parcels.stream()
                    .map(parcel -> parcel.duplicate(parcelId, parcelContentId, false))
                    .toList())
        .statusHistory(List.of(this.getStatus().duplicate(randomUUID().toString(), taskId)))
        .submissionInstant(this.getSubmissionInstant())
        .build();
  }

  public record ParcelDetectionTaskDiff(ParcelDetectionTask oldTask, ParcelDetectionTask newTask) {}
}
