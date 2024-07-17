package app.bpartners.geojobs.repository.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "community_used_surface")
public class CommunityUsedSurface implements Serializable {
  @Id private String id;

  @Column(name = "used_surface")
  private double usedSurface;

  @Column(name = "usage_datetime")
  private Instant usageDatetime;

  @ManyToOne
  @JoinColumn(name = "id_community_authorization")
  private CommunityAuthorization communityAuthorization;
}
