package app.bpartners.geojobs.repository.model;

import app.bpartners.geojobs.job.model.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

@Entity
@Table(name = "polygon_continue")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PolygonContinue {

    @Id
    @Column(nullable = false,name = "bucket_key")
    private String bucketKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(NAMED_ENUM)
    private Status.ProgressionStatus status;
}
