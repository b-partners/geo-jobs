package school.hei.geojobs.endpoint.rest.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import school.hei.geojobs.endpoint.rest.controller.mapper.ZoneTilingTaskMapper;
import school.hei.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import school.hei.geojobs.endpoint.rest.model.Parcel;
import school.hei.geojobs.endpoint.rest.model.ZoneTilingJob;
import school.hei.geojobs.model.BoundedPageSize;
import school.hei.geojobs.model.PageFromOne;
import school.hei.geojobs.service.ZoneTilingJobService;

@RestController
@AllArgsConstructor
@Slf4j
public class ZoneTilingController {
  private final ZoneTilingJobMapper mapper;
  private final ZoneTilingJobService service;
  private final ZoneTilingTaskMapper zoneTilingTaskMapper;

  @PostMapping("/tilingJobs")
  public ZoneTilingJob tileZone(@RequestBody CreateZoneTilingJob job) {
    return mapper.toRest(service.create(mapper.toDomain(job)));
  }

  @GetMapping("/tilingJobs")
  public List<ZoneTilingJob> getTilingJobs(
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "30") BoundedPageSize pageSize) {
    return service.findAll(page, pageSize).stream().map(mapper::toRest).toList();
  }

  @GetMapping("/tilingJobs/{id}/parcels")
  public List<Parcel> getZTJParcels(@PathVariable("id") String jobId) {
    return service.getAJobParcel(jobId).stream()
        .map(parcel -> zoneTilingTaskMapper.toRest(parcel, jobId))
        .toList();
  }
}
