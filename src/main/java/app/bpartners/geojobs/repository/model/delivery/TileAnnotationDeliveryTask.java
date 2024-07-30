package app.bpartners.geojobs.repository.model.delivery;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.util.ArrayList;

import static app.bpartners.geojobs.repository.model.GeoJobType.TILE_ANNOTATION_DELIVERY;
import static org.hibernate.type.SqlTypes.JSON;

@Slf4j
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@JsonIgnoreProperties({"status"})
@ToString
public class TileAnnotationDeliveryTask extends Task implements Serializable {
    @Id
    private String id;
    private String parcelId;

    @JdbcTypeCode(JSON)
    private Tile tile;

    @Override
    public JobType getJobType() {
        return TILE_ANNOTATION_DELIVERY;
    }

    @Override
    public Task semanticClone() {
        return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
    }
}
