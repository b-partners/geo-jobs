package app.bpartners.geojobs.repository.model.community;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.SurfaceUnit;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

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

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private SurfaceUnit maxSurfaceUnit;

  @OneToMany(fetch = EAGER, mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityAuthorizedZone> authorizedZones;

  @OneToMany(fetch = EAGER, mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityDetectableObjectType> detectableObjectTypes;

  @OneToMany(mappedBy = "communityAuthorizationId", cascade = ALL)
  private List<CommunityUsedSurface> usedSurfaces;

  @OneToMany(mappedBy = "communityOwnerId", fetch = LAZY)
  private List<FullDetection> fullDetections;
}
