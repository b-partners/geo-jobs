package app.bpartners.geojobs.repository.model;

import static org.hibernate.type.SqlTypes.JSON;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Parcel implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private ParcelContent parcelContent;
}
