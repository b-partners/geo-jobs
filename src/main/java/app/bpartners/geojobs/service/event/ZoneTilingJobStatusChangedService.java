package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobFailed;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.PointExtendedImageRequest;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneTilingJobStatusChangedService implements Consumer<ZoneTilingJobStatusChanged> {
  private final JobFinishedMailer<ZoneTilingJob> tilingFinishedMailer;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final StatusChangedHandler statusChangedHandler;
  private final DetectionRepository detectionRepository;
  private final EventProducer eventProducer;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final TilingTaskRepository tilingTaskRepository;
  private final GeometryConverter geometryConverter;
  private final PointExtendedImageRequest pointExtendedImageRequest;

  @Override
  public void accept(ZoneTilingJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    var onSucceededHandler =
        new onSucceededJobHandler(
            eventProducer,
            tilingFinishedMailer,
            zoneDetectionJobService,
            newJob,
            detectionRepository,
            objectConfigurationRepository,
            tilingTaskRepository,
            geometryConverter,
            pointExtendedImageRequest);

    var onFailedHandler = new onFailedJobHandler(eventProducer, newJob);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onSucceededHandler, onFailedHandler);
  }

  private record onSucceededJobHandler(
      EventProducer eventProducer,
      JobFinishedMailer<ZoneTilingJob> tilingFinishedMailer,
      ZoneDetectionJobService zoneDetectionJobService,
      ZoneTilingJob ztj,
      DetectionRepository detectionRepository,
      DetectableObjectConfigurationRepository objectConfigurationRepository,
      TilingTaskRepository tilingTaskRepository,
      GeometryConverter geometryConverter,
      PointExtendedImageRequest pointExtendedImageRequest)
      implements Runnable {

    @Override
    public void run() {
      var zdj = zoneDetectionJobService.saveZDJFromZTJ(ztj);
      var optionalDetection = detectionRepository.findByZtjId(ztj.getId());
      // For now, only detection process triggers ZDJ processing
      if (optionalDetection.isPresent()) {
        var detection = optionalDetection.get();
        var savedDetection =
            detectionRepository.save(detection.toBuilder().zdjId(zdj.getId()).build());
        objectConfigurationRepository.saveAll(
            savedDetection.getDetectableObjectConfigurations().stream()
                .map(
                    objectConfiguration ->
                        objectConfiguration.duplicate(randomUUID().toString(), zdj.getId()))
                .toList());
        eventProducer.accept(
            List.of(ZoneDetectionJobCreated.builder().zoneDetectionJob(zdj).build()));

        if (detection.hasToitureModelName()) {
          var collectedPointWithItsMultiPolygon =
              detection.getProvidedGeoJsonZone().stream()
                  .map(
                      feature -> {
                        var layer =
                            detection.getGeoServerProperties().getGeoServerParameter().getLayers();
                        var restPointFeature = pointExtendedImageRequest.apply(feature, layer);
                        var restPoint = restPointFeature.getGeometry().getPoint();
                        var properties =
                            feature.getProperties() == null
                                ? new HashMap<String, Object>()
                                : feature.getProperties();
                        var zoom =
                            properties.get("zoom") != null
                                ? (Integer) properties.get("zoom")
                                : HOUSES_0.getZoomLevel();
                        var pointDomain =
                            geometryConverter.toFeature(zoom, new HashMap<>(), restPoint);
                        var geometryType = feature.getGeometry().getActualInstance();
                        switch (geometryType) {
                          case Point point -> {
                            var multiPolygonFromPointDomain =
                                geometryConverter.toFeature(
                                    null,
                                    zoom,
                                    properties,
                                    geometryConverter.retrieveNearestRoofMultiPolygon(point));
                            return new HashMap<>(Map.of(pointDomain, multiPolygonFromPointDomain));
                          }

                          case Polygon ignored -> {
                            return getPolygonFeatureDelimitationMap(
                                feature, properties, pointDomain);
                          }

                          case MultiPolygon ignored -> {
                            return getPolygonFeatureDelimitationMap(
                                feature, properties, pointDomain);
                          }
                          default ->
                              throw new IllegalStateException(
                                  "Unexpected geometry type: " + geometryType);
                        }
                      })
                  .flatMap(map -> map.entrySet().stream())
                  .collect(
                      Collectors.toMap(
                          entry -> {
                            try {
                              return new ObjectMapper()
                                  .findAndRegisterModules()
                                  .writeValueAsString(entry.getKey());
                            } catch (JsonProcessingException e) {
                              throw new RuntimeException(e);
                            }
                          },
                          Map.Entry::getValue,
                          (v1, v2) -> v1));
          var detectionWithMultiPolygonFromPoint =
              savedDetection.toBuilder()
                  .providedGeoJsonZone(
                      detection.getProvidedGeoJsonZone().stream()
                          .map(FeatureMapper::toDomainFeature)
                          .toList())
                  .pointDelimitation(new HashMap<>(collectedPointWithItsMultiPolygon))
                  .build();
          detectionRepository.save(detectionWithMultiPolygonFromPoint);
        }
      }
      tilingFinishedMailer.accept(ztj);
      log.info("Finished, mail sent, ztj=" + ztj);
    }

    private HashMap<
            app.bpartners.geojobs.repository.model.Feature,
            app.bpartners.geojobs.repository.model.Feature>
        getPolygonFeatureDelimitationMap(
            Feature feature,
            Map<String, Object> properties,
            app.bpartners.geojobs.repository.model.Feature pointDomain) {
      var delimitationFeature = toDomainFeature(feature, new HashMap<>());
      try {
        properties.put(
            "centroid",
            new ObjectMapper().findAndRegisterModules().writeValueAsString(pointDomain));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      return new HashMap<>(Map.of(pointDomain, delimitationFeature));
    }
  }

  private record onFailedJobHandler(EventProducer eventProducer, ZoneTilingJob failedJob)
      implements Runnable {
    @Override
    public void run() {
      eventProducer.accept(List.of(new ZoneTilingJobFailed(failedJob)));
      log.info("Finished with failed status, ztj=" + failedJob);
    }
  }
}
