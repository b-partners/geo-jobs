package app.bpartners.geojobs.repository.model.tiling;

import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@JsonIgnoreProperties({"status"})
@ToString
public class TilingTask extends Task implements Serializable {
  @ManyToMany(cascade = ALL, fetch = EAGER)
  @JoinTable(
      name = "parcel_tiling_task",
      joinColumns = @JoinColumn(name = "id_tiling_task"),
      inverseJoinColumns = @JoinColumn(name = "id_parcel"))
  private List<Parcel> parcels;

  @Override
  public GeoJobType getJobType() {
    return TILING;
  }

  @Override
  public Task semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }

  public ParcelContent getParcelContent() {
    return getParcel().getParcelContent();
  }

  public String getParcelId() {
    return parcels.isEmpty() ? null : getParcel().getId();
  }

  public String getParcelContentId() {
    return parcels.isEmpty() ? null : getParcelContent().getId();
  }

  public Parcel getParcel() {
    if (parcels.isEmpty()) return null;
    var chosenParcel = parcels.get(0);
    if (parcels.size() > 1) {
      log.error(
          "[DEBUG] TilingTask(id={}) contains multiple parcels (size= {}) but only one"
              + " Parcel(id={}) is handle for now",
          getId(),
          parcels.size(),
          chosenParcel.getId());
    }
    return chosenParcel;
  }

  public TilingTask duplicate(
      String taskId, String jobId, String parcelId, String parcelContentId) {
    return TilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(
            parcels.stream().map(parcel -> parcel.duplicate(parcelId, parcelContentId)).toList())
        .statusHistory(
            this.getStatusHistory().stream()
                .map(status -> status.duplicate(randomUUID().toString(), taskId))
                .toList())
        .submissionInstant(this.getSubmissionInstant())
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TilingTask that = (TilingTask) o;
    return Objects.equals(parcels, that.parcels)
        && Objects.equals(getJobId(), that.getJobId())
        && Objects.equals(getStatusHistory(), that.getStatusHistory());
  }

  @Override
  public int hashCode() {
    return Objects.hash(parcels, getJobId(), getStatusHistory());
  }
}
