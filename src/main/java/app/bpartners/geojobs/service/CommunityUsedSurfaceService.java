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

  public Optional<CommunityUsedSurface> getLastUsedSurfaceByCommunityApiKey(String apikey) {
    var page = Pageable.ofSize(1).withPage(0);
    return communityUsedSurfaceRepository
        .findByCommunityAuthorization_ApiKeyOrderByUsageDatetimeDesc(apikey, page)
        .stream()
        .findFirst();
  }

  public CommunityUsedSurface appendLastUsedSurface(CommunityUsedSurface communityUsedSurface) {
    var lastCommunityUsedSurface =
        this.getLastUsedSurfaceByCommunityApiKey(
            communityUsedSurface.getCommunityAuthorization().getApiKey());
    var lastCommunityUsedSurfaceValue =
        lastCommunityUsedSurface.map(CommunityUsedSurface::getUsedSurface).orElse(0.0);
    var usedSurfaceValueToAppend = communityUsedSurface.getUsedSurface();
    var newLastUsedSurfaceValue = lastCommunityUsedSurfaceValue + usedSurfaceValueToAppend;

    return communityUsedSurfaceRepository.save(
        CommunityUsedSurface.builder()
            .id(communityUsedSurface.getId())
            .usedSurface(newLastUsedSurfaceValue)
            .usageDatetime(Instant.now())
            .communityAuthorization(communityUsedSurface.getCommunityAuthorization())
            .build());
  }
}
