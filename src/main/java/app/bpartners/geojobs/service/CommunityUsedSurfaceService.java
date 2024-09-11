package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_DEGREE;
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
  private final CommunityAuthorizationRepository caRepository;
  private final DetectionSurfaceValueMapper surfaceValueMapper;

  public Optional<CommunityUsedSurface> getTotalUsedSurfaceByCommunityId(String communityId) {
    return getTotalUsedSurfaceByCommunityId(communityId, SQUARE_DEGREE);
  }

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
        lastTotalUsedSurface.map(CommunityUsedSurface::getUsedSurface).orElse(0.0);
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
        caRepository.findByApiKey(principal.getPassword()).orElseThrow(ForbiddenException::new);
    var totalUsedSurface =
        getTotalUsedSurfaceByCommunityId(communityAuthorization.getId(), unit)
            .orElse(
                CommunityUsedSurface.builder()
                    .usedSurface(0)
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
