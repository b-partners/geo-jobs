package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.service.GeoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GeoCodeController {
  private final GeoCodeService service;

  @GetMapping("/geocode")
  public Feature getGeocode(@RequestParam("address") String address) {
    return toRestFeature(service.geocode(address));
  }
}
