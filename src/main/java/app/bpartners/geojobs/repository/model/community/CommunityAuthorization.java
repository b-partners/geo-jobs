package app.bpartners.geojobs.repository.model.community;

import static jakarta.persistence.CascadeType.ALL;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "community_authorization")
public class CommunityAuthorization implements Serializable {
  @Id private String id;

  @Column private String name;

  @Column(name = "api_key")
  private String apiKey;

  @Column(name = "max_surface")
  private double maxSurface;

  @OneToMany(mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityZone> authorizedZones;

  @OneToMany(mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityDetectableObjectType> detectableObjectTypes;

  @OneToMany(mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityUsedSurface> usedSurfaces;
}
