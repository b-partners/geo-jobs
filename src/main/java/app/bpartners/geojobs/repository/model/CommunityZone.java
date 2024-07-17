package app.bpartners.geojobs.repository.model;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@Entity(name = "communit_zone")
public class CommunityZone implements Serializable {
  @Id private String id;

  @Column private String name;

  @ManyToOne
  @JoinColumn(name = "id_community_authorization")
  private CommunityAuthorization communityAuthorization;
}
