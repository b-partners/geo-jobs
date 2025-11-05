package app.bpartners.geojobs.repository.model.cityjson;

import static java.time.Instant.now;

import app.bpartners.geojobs.repository.model.detection.Detection;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cityjson")
public class CityJSON {
  @Id private String id;

  @Column(nullable = false)
  private String s3FileKey;

  @Column(updatable = false)
  private Instant creationDatetime;

  @PrePersist
  protected void onCreate() {
    this.creationDatetime = now().truncatedTo(ChronoUnit.MICROS);
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "detection_id")
  private Detection detection;
}
