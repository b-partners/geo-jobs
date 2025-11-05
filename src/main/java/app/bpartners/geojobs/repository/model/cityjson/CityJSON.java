package app.bpartners.geojobs.repository.model.cityjson;

import app.bpartners.geojobs.repository.model.Feature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import static org.hibernate.type.SqlTypes.JSON;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "city_json")
@Entity
public class CityJSON {
    @Id private String id;

    @Column(name = "delimitation")
    @JdbcTypeCode(JSON)
    private Feature delimitation;

    @Column(name = "s3_file_key", nullable = false)
    private String s3FileKey;
}
