package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.CommunityUsedSurfaceRepository;
import app.bpartners.geojobs.repository.model.CommunityUsedSurface;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityUsedSurfaceService {
  private final CommunityUsedSurfaceRepository communityUsedSurfaceRepository;

  public Optional<CommunityUsedSurface> getLastUsedSurfaceByApiKey(String apikey) {
    var page = Pageable.ofSize(1).withPage(1);
    return communityUsedSurfaceRepository
        .findByCommunityAuthorization_ApiKeyOrderByUsageDatetimeDesc(apikey, page)
        .stream()
        .findFirst();
  }

  public CommunityUsedSurface appendUsedSurfaceByApiKey(CommunityUsedSurface communityUsedSurface) {
    var lastCommunityUsedSurface =
        this.getLastUsedSurfaceByApiKey(
            communityUsedSurface.getCommunityAuthorization().getApiKey());
    var lastCommunityUsedSurfaceValue =
        lastCommunityUsedSurface.map(CommunityUsedSurface::getUsedSurface).orElse(0.0);
    var usedSurfaceToAppend = communityUsedSurface.getUsedSurface();
    var newLastUsedSurface = lastCommunityUsedSurfaceValue + usedSurfaceToAppend;

    return communityUsedSurfaceRepository.save(
        CommunityUsedSurface.builder()
            .usedSurface(newLastUsedSurface)
            .usageDatetime(Instant.now())
            .communityAuthorization(communityUsedSurface.getCommunityAuthorization())
            .build());
  }
}
