package app.bpartners.geojobs.repository.model.tiling;

import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
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
@EqualsAndHashCode(callSuper = false)
public class TilingTask extends Task implements Serializable {
  @ManyToMany(cascade = ALL, fetch = LAZY)
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
  public String toString() {
    return "TilingTask{" + "parcel=" + getParcel() + ", status=" + getStatus() + '}';
  }

  public ParcelContent getParcelContent() {
    return getParcel().getParcelContent();
  }

  public Parcel getParcel() {
    if (parcels.isEmpty()) return null;
    var chosenParcel = parcels.get(0);
    if (parcels.size() > 1) {
      log.info(
          "TilingTask(id={}) contains multiple parcels but only one Parcel(id={}) is handle for"
              + " now",
          getId(),
          chosenParcel.getId());
    }
    return chosenParcel;
  }
}
