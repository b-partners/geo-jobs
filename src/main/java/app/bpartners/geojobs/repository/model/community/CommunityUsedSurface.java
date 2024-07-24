package app.bpartners.geojobs.repository.model.community;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "community_used_surface")
public class CommunityUsedSurface implements Serializable {
  @Id private String id;

  @Column(name = "used_surface")
  private double usedSurface;

  @Column(name = "usage_datetime")
  private Instant usageDatetime;

  @JoinColumn(referencedColumnName = "id")
  private String communityAuthorizationId;
}
