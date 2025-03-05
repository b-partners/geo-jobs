package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.AreaPictureMapper;
import app.bpartners.geojobs.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.geojobs.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.service.AreaPictureMapLayerService;
import app.bpartners.geojobs.service.AreaPictureService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AreaPictureController {
  private final AreaPictureService service;
  private final AreaPictureMapLayerService mapLayerService;
  private final AreaPictureMapper mapper;

  @PutMapping(value = "/areaPicture")
  public AreaPictureDetails crupdateAreaPicture(@RequestBody CrupdateAreaPictureDetails toCreate) {
    var toSave = mapper.toDomain(toCreate);
    var actualLayer = mapLayerService.getById(toCreate.getLayerId());
    var otherLayers = mapLayerService.findOtherLayers(toSave);
    return mapper.toRest(service.crupdate(toSave), actualLayer, otherLayers);
  }
}
