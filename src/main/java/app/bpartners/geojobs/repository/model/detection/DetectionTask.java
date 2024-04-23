package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

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
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@JsonIgnoreProperties({"status"})
public class DetectionTask extends Task implements Serializable {
  @ManyToMany(cascade = ALL, fetch = LAZY)
  @JoinTable(
      name = "parcel_detection_task",
      joinColumns = @JoinColumn(name = "id_detection_task"),
      inverseJoinColumns = @JoinColumn(name = "id_parcel"))
  private List<Parcel> parcels;

  public Tile getTile() {
    return getParcel() == null ? null : getParcel().getParcelContent().getFirstTile();
  }

  public List<Tile> getTiles() {
    return getParcel() == null ? null : getParcel().getParcelContent().getTiles();
  }

  public Parcel getParcel() {
    if (parcels.isEmpty()) return null;
    var chosenParcel = parcels.get(0);
    if (parcels.size() > 1) {
      log.info(
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
}
