package app.bpartners.geojobs.service.dashboard;

import static java.math.RoundingMode.HALF_UP;
import static java.time.Instant.now;

import app.bpartners.geojobs.repository.CachedAreaPictureMapLayerRepository;
import app.bpartners.geojobs.repository.model.CachedAreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AreaPictureMapLayerResolver {
  static final int CACHE_KEY_COORDINATES_SCALE = 3;
  static final Duration CACHE_ENTRY_LIFETIME = Duration.ofDays(30);

  private final AreaPictureApi areaPictureApi;
  private final CachedAreaPictureMapLayerRepository cachedMapLayerRepository;

  public AreaPictureMapLayer apply(
      double longitude, double latitude, Supplier<String> dashboardApiKey) {
    var cacheKey = cacheKeyOf(longitude, latitude);
    var cached = cachedMapLayerRepository.findById(cacheKey).filter(this::isStillFresh);
    if (cached.isPresent()) {
      return toMapLayer(cached.get());
    }

    var retrieved =
        areaPictureApi
            .getAreaPictureMapLayers(longitude, latitude, dashboardApiKey.get())
            .getFirst();
    cachedMapLayerRepository.save(
        CachedAreaPictureMapLayer.builder()
            .id(cacheKey)
            .layerName(retrieved.name())
            .precisionLevelInCm(retrieved.precisionLevelInCm())
            .cachedAt(now())
            .build());
    return retrieved;
  }

  private boolean isStillFresh(CachedAreaPictureMapLayer cached) {
    return cached.getCachedAt() != null
        && Duration.between(cached.getCachedAt(), now()).compareTo(CACHE_ENTRY_LIFETIME) < 0;
  }

  private AreaPictureMapLayer toMapLayer(CachedAreaPictureMapLayer cached) {
    return new AreaPictureMapLayer(
        null, cached.getLayerName(), null, cached.getPrecisionLevelInCm());
  }

  static String cacheKeyOf(double longitude, double latitude) {
    return rounded(longitude) + "," + rounded(latitude);
  }

  private static String rounded(double coordinate) {
    return BigDecimal.valueOf(coordinate)
        .setScale(CACHE_KEY_COORDINATES_SCALE, HALF_UP)
        .toPlainString();
  }
}
