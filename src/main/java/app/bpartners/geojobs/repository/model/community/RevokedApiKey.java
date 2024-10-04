package app.bpartners.geojobs.repository.model.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "revoked_api_key")
public class RevokedApiKey implements Serializable {
    @Id private String id;

    @Column private String apiKey;

    @Column private Instant revokedAt;

    @JoinColumn(referencedColumnName = "id", name = "community_owner_id")
    private String communityOwnerId;
}
