package app.bpartners.geojobs.repository.model.community;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "community_zone")
public class CommunityZone implements Serializable {
  @Id private String id;

  @Column private String name;

  @JoinColumn(referencedColumnName = "id")
  private String communityAuthorizationId;
}
