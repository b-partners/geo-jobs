package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.security.ApiKey;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  public List<ApiKey> generateApiKeys(List<CommunityAuthorization> authorizations) {
    return communityAuthorizationRepository.saveAll(authorizations).stream()
        .map(
            communityAuthorization ->
                new ApiKey(
                    communityAuthorization.getApiKey(),
                    communityAuthorization.getCreationDatetime()))
        .collect(Collectors.toList());
  }
}
