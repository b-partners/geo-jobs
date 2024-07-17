package app.bpartners.geojobs.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "community_detectable_object_type")
public class CommunityDetectableObjectType implements Serializable {
  @Id private String id;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectableType type;

  @ManyToOne
  @JoinColumn(name = "id_community_authorization")
  private CommunityAuthorization communityAuthorization;
}
