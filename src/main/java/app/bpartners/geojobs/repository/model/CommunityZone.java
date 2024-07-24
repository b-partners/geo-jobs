package app.bpartners.geojobs.repository.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "community_zone")
public class CommunityZone implements Serializable {
  @Id private String id;

  @Column private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_community_authorization")
  private CommunityAuthorization communityAuthorization;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommunityZone that = (CommunityZone) o;
    return Objects.equals(id, that.id) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "CommunityZone{" + "name='" + name + '\'' + ", id='" + id + '\'' + '}';
  }
}
