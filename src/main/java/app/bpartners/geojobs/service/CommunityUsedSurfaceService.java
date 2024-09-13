package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionSurfaceValueMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectionUsage;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.CommunityUsedSurfaceRepository;
import app.bpartners.geojobs.repository.model.SurfaceUnit;
import app.bpartners.geojobs.repository.model.community.CommunityUsedSurface;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityUsedSurfaceService {
  private final CommunityUsedSurfaceRepository communityUsedSurfaceRepository;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final DetectionSurfaceValueMapper surfaceValueMapper;
  private static final double DEFAULT_USED_SURFACE_VALUE = 0.0;

  public Optional<CommunityUsedSurface> getTotalUsedSurfaceByCommunityId(
      String communityId, SurfaceUnit unit) {
    var page = Pageable.ofSize(1).withPage(0);
    return communityUsedSurfaceRepository
        .findByCommunityAuthorizationIdOrderByUsageDatetimeDesc(communityId, page)
        .stream()
        .map(usedSurface -> convertTo(usedSurface, unit))
        .findFirst();
  }

  public CommunityUsedSurface appendLastUsedSurface(CommunityUsedSurface newUsedSurface) {
    var lastTotalUsedSurface =
        this.getTotalUsedSurfaceByCommunityId(
            newUsedSurface.getCommunityAuthorizationId(), newUsedSurface.getUnit());
    var lastTotalUsedSurfaceValue =
        lastTotalUsedSurface
            .map(CommunityUsedSurface::getUsedSurface)
            .orElse(DEFAULT_USED_SURFACE_VALUE);
    var newLastTotalUsedSurfaceValue = lastTotalUsedSurfaceValue + newUsedSurface.getUsedSurface();

    return communityUsedSurfaceRepository.save(
        CommunityUsedSurface.builder()
            .id(randomUUID().toString())
            .usedSurface(newLastTotalUsedSurfaceValue)
            .usageDatetime(now())
            .unit(newUsedSurface.getUnit())
            .communityAuthorizationId(newUsedSurface.getCommunityAuthorizationId())
            .build());
  }

  public DetectionUsage getUsage(Principal principal, SurfaceUnit unit) {
    var communityAuthorization =
        communityAuthRepository
            .findByApiKey(principal.getPassword())
            .orElseThrow(ForbiddenException::new);
    var totalUsedSurface =
        getTotalUsedSurfaceByCommunityId(communityAuthorization.getId(), unit)
            .orElse(
                CommunityUsedSurface.builder()
                    .usedSurface(DEFAULT_USED_SURFACE_VALUE)
                    .unit(unit)
                    .usageDatetime(now())
                    .build());
    var maxAuthorizedSurface =
        CommunityUsedSurface.builder()
            .usedSurface(communityAuthorization.getMaxSurface())
            .unit(communityAuthorization.getMaxSurfaceUnit())
            .build();
    var remainingSurface =
        convertTo(maxAuthorizedSurface, unit).getUsedSurface()
            - convertTo(totalUsedSurface, unit).getUsedSurface();

    return new DetectionUsage()
        .totalUsedSurface(
            surfaceValueMapper.toSurfaceValue(totalUsedSurface.getUsedSurface(), unit))
        .remainingSurface(surfaceValueMapper.toSurfaceValue(remainingSurface, unit))
        .maxAuthorizedSurface(
            surfaceValueMapper.toSurfaceValue(maxAuthorizedSurface.getUsedSurface(), unit))
        .lastDatetimeSurfaceUsage(totalUsedSurface.getUsageDatetime());
  }

  public CommunityUsedSurface convertTo(
      CommunityUsedSurface communityUsedSurface, SurfaceUnit unit) {
    if (communityUsedSurface.getUnit().equals(unit)) {
      return communityUsedSurface;
    }
    throw new NotImplementedException("Conversion of surface units is not supported yet");
  }
}
