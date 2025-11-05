package app.bpartners.geojobs.repository.model.cityjson;

import app.bpartners.geojobs.repository.model.Feature;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static jakarta.persistence.EnumType.STRING;
import static java.time.Instant.now;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "city_json_request")
public class CityJSONRequest implements Serializable {
    @Id private String id;

    @Column(name = "creation_datetime")
    private Instant creationDatetime;

    @JoinColumn(referencedColumnName = "id", name = "community_owner_id")
    private String communityOwnerId;

    @Column(name = "delimitations")
    @JdbcTypeCode(JSON)
    private List<Feature> delimitations;

    @Enumerated(STRING)
    @JdbcTypeCode(NAMED_ENUM)
    private CityJSONRequestStatus status;

    @PrePersist
    protected void onCreate() {
        this.creationDatetime = now().truncatedTo(ChronoUnit.MICROS);
    }
}
