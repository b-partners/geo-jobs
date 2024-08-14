package app.bpartners.geojobs.repository.model.community;

import static jakarta.persistence.CascadeType.ALL;
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

  @Column private String apiKey;

  @Column private double maxSurface;

  @OneToMany(fetch = EAGER, mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityAuthorizedZone> authorizedZones;

  @OneToMany(fetch = EAGER, mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityDetectableObjectType> detectableObjectTypes;

  @OneToMany(mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityUsedSurface> usedSurfaces;
}