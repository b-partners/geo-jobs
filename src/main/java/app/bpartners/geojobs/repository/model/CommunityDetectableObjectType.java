package app.bpartners.geojobs.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "community_detectable_object_type")
public class CommunityDetectableObjectType implements Serializable {
  @Id private String id;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectableType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_community_authorization")
  private CommunityAuthorization communityAuthorization;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommunityDetectableObjectType that = (CommunityDetectableObjectType) o;
    return Objects.equals(id, that.id) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type);
  }

  @Override
  public String toString() {
    return "CommunityDetectableObjectType{" + "id='" + id + '\'' + ", type=" + type + '}';
  }
}
