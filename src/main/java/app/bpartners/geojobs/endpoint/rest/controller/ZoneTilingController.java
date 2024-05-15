package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.SuccessStatus.NOT_SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.SuccessStatus.SUCCEEDED;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TilingTaskMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoomMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
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
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Slf4j
public class ZoneTilingController {
  private final ZoneTilingJobService service;
  private final ParcelService parcelService;
  private final ZoneTilingJobMapper mapper;
  private final ZoomMapper zoomMapper;
  private final TilingTaskMapper tilingTaskMapper;
  private final TaskStatisticMapper taskStatisticMapper;

  @GetMapping("/tilingJobs/{id}/taskStatistics")
  public TaskStatistic getTilingTaskStatistics(@PathVariable String id) {
    return taskStatisticMapper.toRest(service.computeTaskStatistics(id));
  }

  @PostMapping("/tilingJobs")
  public ZoneTilingJob tileZone(@RequestBody CreateZoneTilingJob createJob) {
    var job = mapper.toDomain(createJob);
    var tilingTasks = getTilingTasks(createJob, job.getId());
    return mapper.toRest(service.create(job, tilingTasks), tilingTasks);
  }

  @PostMapping("/tilingJobs/{id}/taskFiltering")
  public List<FilteredTilingJob> filterTilingTasks(@PathVariable String id) {
    return service.dispatchTasksBySuccessStatus(id).stream()
        .map(
            job ->
                new FilteredTilingJob()
                    .status(job.isSucceeded() ? SUCCEEDED : NOT_SUCCEEDED)
                    .job(mapper.toRest(job, List.of())))
        .toList();
  }

  @PostMapping("/tilingJobs/{id}/duplications")
  public ZoneTilingJob duplicateTilingJob(@PathVariable String id) {
    return mapper.toRest(service.duplicate(id), List.of());
  }

  @PutMapping("/tilingJobs/{id}/retry")
  public ZoneTilingJob processFailedTilingJob(@PathVariable String id) {
    return mapper.toRest(
        service.retryFailedTask(id), List.of()); // TODO: check if features must be returned
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
    return service.findAll(page, pageSize).stream()
        .map(job -> mapper.toRest(job, List.of())) // Features ignored when listing tiling jobs
        .toList();
  }

  @GetMapping("/tilingJobs/{id}/parcels")
  public List<Parcel> getZTJParcels(@PathVariable("id") String jobId) {
    return parcelService.getParcelsByJobId(jobId).stream().map(tilingTaskMapper::toRest).toList();
  }
}
