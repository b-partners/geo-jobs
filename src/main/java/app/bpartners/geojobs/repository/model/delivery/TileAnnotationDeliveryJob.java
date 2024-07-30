package app.bpartners.geojobs.repository.model.delivery;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;

import static app.bpartners.geojobs.repository.model.GeoJobType.TILE_ANNOTATION_DELIVERY;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Entity
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Data
@JsonIgnoreProperties({"status"})
@ToString
public class TileAnnotationDeliveryJob extends Job implements Serializable {
    @Override
    protected JobType getType() {
        return TILE_ANNOTATION_DELIVERY;
    }

    @Override
    public Job semanticClone() {
        return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
    }
}
