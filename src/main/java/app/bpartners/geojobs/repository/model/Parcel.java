package app.bpartners.geojobs.repository.model;

import static org.hibernate.type.SqlTypes.JSON;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Parcel implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private ParcelContent parcelContent;

  public Parcel duplicate(String parcelId, String parcelContentId) {
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(parcelContent.duplicate(parcelContentId))
        .build();
  }
}
