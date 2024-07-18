package app.bpartners.geojobs.repository.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.*;

@Getter
@Setter
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

  @ManyToOne
  @JoinColumn(name = "id_community_authorization")
  private CommunityAuthorization communityAuthorization;

  @Override
  public String toString() {
    return "CommunityUsedSurface{"
        + "id='"
        + id
        + '\''
        + ", usedSurface="
        + usedSurface
        + ", usageDatetime="
        + usageDatetime
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommunityUsedSurface that = (CommunityUsedSurface) o;
    return Double.compare(usedSurface, that.usedSurface) == 0
        && Objects.equals(id, that.id)
        && Objects.equals(usageDatetime, that.usageDatetime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, usedSurface, usageDatetime);
  }
}
