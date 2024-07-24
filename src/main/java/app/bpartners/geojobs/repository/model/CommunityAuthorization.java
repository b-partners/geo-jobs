package app.bpartners.geojobs.repository.model;

import static jakarta.persistence.FetchType.EAGER;

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

  @OneToMany(fetch = EAGER, mappedBy = "communityAuthorization")
  private List<CommunityZone> authorizedZones;

  @OneToMany(fetch = EAGER, mappedBy = "communityAuthorization")
  private List<CommunityDetectableObjectType> detectableObjectTypes;

  @OneToMany(fetch = EAGER, mappedBy = "communityAuthorization")
  private List<CommunityUsedSurface> usedSurfaces;
}
