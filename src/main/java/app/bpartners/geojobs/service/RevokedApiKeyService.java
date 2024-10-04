package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.RevokedApiKeyRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
public class RevokedApiKeyService {
    private final RevokedApiKeyRepository repository;
    private final CommunityAuthorizationRepository communityAuthRepository;

    @Transactional
    public void revokeCommunityApiKey(CommunityAuthorization communityAuthorization){
        var revokedApiKey = RevokedApiKey
            .builder()
                .id(randomUUID().toString())
                .revokedAt(now())
                .apiKey(communityAuthorization.getApiKey())
                .communityOwnerId(communityAuthorization.getId())
            .build();

        communityAuthorization.setRevoked(true);
        repository.save(revokedApiKey);
        communityAuthRepository.save(communityAuthorization);
    }
}
