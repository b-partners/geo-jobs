package app.bpartners.geojobs.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.ArcgisRasterZoom;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.parcelization.ParcelizedPolygon;
import app.bpartners.geojobs.model.parcelization.area.SquareDegree;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ParcelService {
  private static final int DEFAULT_ZOOM = 20;
  private final TilingTaskRepository tilingTaskRepository;
  private final ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobRepository detectionJobRepository;
  private final FeatureMapper featureMapper;
  private final ParcelizedPolygonRepository parcelizedPolygonRepository;

  @Transactional
  public List<Parcel> getParcelsByJobId(String jobId) {
    // TODO: refactor duplicated computing
    var zoneTilingJob = tilingJobRepository.findById(jobId);
    if (zoneTilingJob.isPresent()) {
      List<TilingTask> duplicatedTilingTasks =
          tilingTaskRepository.findAllByJobId(jobId).stream()
              .map(
                  task -> {
                    boolean hasSameStatuses = true;
                    boolean hasSameTile = true;
                    return task.duplicate(
                        task.getId(),
                        task.getJobId(),
                        task.getParcelId(),
                        task.getParcelContentId(),
                        hasSameStatuses,
                        hasSameTile);
                  })
              .toList();
      return duplicatedTilingTasks.stream()
          .map(
              tilingTask -> {
                var parcel = tilingTask.getParcel();
                if (parcel != null) {
                  var parcelContent = parcel.getParcelContent();
                  parcelContent.setTilingStatus(tilingTask.getStatus());
                  return parcel;
                }
                return null;
              })
          .toList();
    }

    var zoneDetectionJob = detectionJobRepository.findById(jobId);
    if (zoneDetectionJob.isPresent()) {
      return parcelDetectionTaskRepository.findAllByJobId(jobId).stream()
          .map(
              detectionTask -> {
                var parcel = detectionTask.getParcel();
                if (parcel != null) {
                  var parcelContent = parcel.getParcelContent();
                  parcelContent.setDetectionStatus(detectionTask.getStatus());
                  return parcel;
                }
                return null;
              })
          .toList();
    }

    throw new NotFoundException("jobId=" + jobId);
  }

  public List<Feature> parcelizeFeature(
      Polygon polygon, Integer referenceZoom, Integer targetZoom, Double maxParcelArea, String id) {
    int refZoom = referenceZoom != null ? referenceZoom : DEFAULT_ZOOM;
    ParcelizedPolygon parcelizedPolygon;
    if (targetZoom == null || maxParcelArea == null) {
      parcelizedPolygon = new ParcelizedPolygon(polygon, new ArcgisRasterZoom(refZoom));
    } else {
      parcelizedPolygon =
          new ParcelizedPolygon(
              polygon,
              new ArcgisRasterZoom(targetZoom),
              new ArcgisRasterZoom(referenceZoom),
              new SquareDegree(maxParcelArea));
    }

    ArrayList<Polygon> parcels = new ArrayList<>(parcelizedPolygon.getParcels());
    List<Feature> features =
        parcels.stream().map(polygonParcel -> featureMapper.toRest(polygonParcel, id)).toList();
    List<app.bpartners.geojobs.repository.model.ParcelizedPolygon> parcelizedPolygons =
        features.stream()
            .map(
                feat ->
                    app.bpartners.geojobs.repository.model.ParcelizedPolygon.builder()
                        .feature(feat)
                        .id(randomUUID().toString())
                        .build())
            .toList();
    parcelizedPolygonRepository.saveAll(parcelizedPolygons);

    return features;
  }
}
