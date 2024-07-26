package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.CommunityUsedSurfaceRepository;
import app.bpartners.geojobs.repository.model.community.CommunityUsedSurface;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityUsedSurfaceService {
  private final CommunityUsedSurfaceRepository communityUsedSurfaceRepository;

  public Optional<CommunityUsedSurface> getLastUsedSurfaceByCommunityId(String communityId) {
    var page = Pageable.ofSize(1).withPage(0);
    return communityUsedSurfaceRepository
        .findByCommunityAuthorizationIdOrderByUsageDatetimeDesc(communityId, page)
        .stream()
        .findFirst();
  }

  public CommunityUsedSurface appendLastUsedSurface(
      String communityId, double newUsedSurfaceValue) {
    var lastCommunityUsedSurface = this.getLastUsedSurfaceByCommunityId(communityId);
    var lastCommunityUsedSurfaceValue =
        lastCommunityUsedSurface.map(CommunityUsedSurface::getUsedSurface).orElse(0.0);
    var newLastUsedSurfaceValue = lastCommunityUsedSurfaceValue + newUsedSurfaceValue;

    return communityUsedSurfaceRepository.save(
        CommunityUsedSurface.builder()
            .id(UUID.randomUUID().toString())
            .usedSurface(newLastUsedSurfaceValue)
            .usageDatetime(Instant.now())
            .communityAuthorizationId(communityId)
            .build());
  }
}
