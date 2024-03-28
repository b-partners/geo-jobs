package app.bpartners.geojobs.endpoint.rest.controller;

import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.TilingTaskMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoomMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.Parcel;
import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.service.ParcelService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.net.URL;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class ZoneTilingController {
  private final ZoneTilingJobService service;
  private final ParcelService parcelService;
  private final ZoneTilingJobMapper mapper;
  private final ZoomMapper zoomMapper;
  private final TilingTaskMapper tilingTaskMapper;

  @PostMapping("/tilingJobs")
  public ZoneTilingJob tileZone(@RequestBody CreateZoneTilingJob createJob) {
    var job = mapper.toDomain(createJob);
    return mapper.toRest(service.create(job, getTilingTasks(createJob, job.getId())));
  }

  @PostMapping("/tilingJobs/{id}/duplications")
  public ZoneTilingJob duplicateTilingJob(@PathVariable String id) {
    return mapper.toRest(service.duplicate(id));
  }

  @SneakyThrows
  private List<TilingTask> getTilingTasks(CreateZoneTilingJob job, String jobId) {
    var serverUrl = new URL(job.getGeoServerUrl());
    return job.getFeatures().stream()
        .map(
            feature -> {
              feature.setZoom(zoomMapper.toDomain(job.getZoomLevel()).getZoomLevel());
              return tilingTaskMapper.from(feature, serverUrl, job.getGeoServerParameter(), jobId);
            })
        .collect(toList());
  }

  @GetMapping("/tilingJobs")
  public List<ZoneTilingJob> getTilingJobs(
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "30") BoundedPageSize pageSize) {
    return service.findAll(page, pageSize).stream().map(mapper::toRest).toList();
  }

  @GetMapping("/tilingJobs/{id}/parcels")
  public List<Parcel> getZTJParcels(@PathVariable("id") String jobId) {
    return parcelService.getParcelsByJobId(jobId).stream().map(tilingTaskMapper::toRest).toList();
  }
}
