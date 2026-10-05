package app.bpartners.geojobs.service.dashboard;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.CachedAreaPictureMapLayerRepository;
import app.bpartners.geojobs.repository.model.CachedAreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.Zoom;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AreaPictureMapLayerResolverTest {
  private static final double LONGITUDE = -0.2494458;
  private static final double LATITUDE = 46.65203;
  private static final String EXPECTED_CACHE_KEY = "-0.249,46.652";

  private final AreaPictureApi areaPictureApiMock = mock();
  private final CachedAreaPictureMapLayerRepository cachedMapLayerRepositoryMock = mock();
  private final AreaPictureMapLayerResolver subject =
      new AreaPictureMapLayerResolver(areaPictureApiMock, cachedMapLayerRepositoryMock);

  private final AreaPictureMapLayer retrievedLayer =
      new AreaPictureMapLayer("layer-id", "ALPES-MARITIMES_2024_5cm", new Zoom("HOUSES_0", 20), 5);

  @Test
  void calls_the_dashboard_api_and_caches_the_layer_on_a_miss() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY)).thenReturn(Optional.empty());
    when(areaPictureApiMock.getAreaPictureMapLayers(LONGITUDE, LATITUDE, "dashboard-key"))
        .thenReturn(List.of(retrievedLayer));

    var actual = subject.apply(LONGITUDE, LATITUDE, () -> "dashboard-key");

    assertEquals("ALPES-MARITIMES_2024_5cm", actual.name());
    assertEquals(5, actual.precisionLevelInCm());
    var cachedCaptor = ArgumentCaptor.forClass(CachedAreaPictureMapLayer.class);
    verify(cachedMapLayerRepositoryMock).save(cachedCaptor.capture());
    var cached = cachedCaptor.getValue();
    assertEquals(EXPECTED_CACHE_KEY, cached.getId());
    assertEquals("ALPES-MARITIMES_2024_5cm", cached.getLayerName());
    assertEquals(5, cached.getPrecisionLevelInCm());
  }

  @Test
  void does_not_call_the_dashboard_api_nor_resolve_the_api_key_on_a_hit() {
    var apiKeyResolutions = new AtomicInteger();
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY))
        .thenReturn(Optional.of(cachedLayerAgedOf(Duration.ofDays(1))));

    var actual =
        subject.apply(
            LONGITUDE,
            LATITUDE,
            () -> {
              apiKeyResolutions.incrementAndGet();
              return "dashboard-key";
            });

    assertEquals("ALPES-MARITIMES_2024_5cm", actual.name());
    assertEquals(5, actual.precisionLevelInCm());
    assertEquals(0, apiKeyResolutions.get());
    verifyNoInteractions(areaPictureApiMock);
    verify(cachedMapLayerRepositoryMock, never()).save(any());
  }

  @Test
  void shares_one_cache_entry_between_coordinates_of_the_same_rounded_cell() {
    var nearbyLongitude = LONGITUDE + 0.0003;
    var nearbyLatitude = LATITUDE + 0.0002;

    assertEquals(
        AreaPictureMapLayerResolver.cacheKeyOf(LONGITUDE, LATITUDE),
        AreaPictureMapLayerResolver.cacheKeyOf(nearbyLongitude, nearbyLatitude));
    assertFalse(
        AreaPictureMapLayerResolver.cacheKeyOf(LONGITUDE, LATITUDE)
            .equals(AreaPictureMapLayerResolver.cacheKeyOf(LONGITUDE + 0.01, LATITUDE)));
  }

  @Test
  void calls_the_dashboard_api_again_once_the_cached_layer_expired() {
    when(cachedMapLayerRepositoryMock.findById(EXPECTED_CACHE_KEY))
        .thenReturn(
            Optional.of(
                cachedLayerAgedOf(AreaPictureMapLayerResolver.CACHE_ENTRY_LIFETIME.plusDays(1))));
    when(areaPictureApiMock.getAreaPictureMapLayers(anyDouble(), anyDouble(), anyString()))
        .thenReturn(List.of(retrievedLayer));

    var actual = subject.apply(LONGITUDE, LATITUDE, () -> "dashboard-key");

    assertEquals("ALPES-MARITIMES_2024_5cm", actual.name());
    verify(areaPictureApiMock, times(1))
        .getAreaPictureMapLayers(eq(LONGITUDE), eq(LATITUDE), eq("dashboard-key"));
    verify(cachedMapLayerRepositoryMock, times(1)).save(any());
  }

  private CachedAreaPictureMapLayer cachedLayerAgedOf(Duration age) {
    return CachedAreaPictureMapLayer.builder()
        .id(EXPECTED_CACHE_KEY)
        .layerName("ALPES-MARITIMES_2024_5cm")
        .precisionLevelInCm(5)
        .cachedAt(now().minus(age))
        .build();
  }
}
