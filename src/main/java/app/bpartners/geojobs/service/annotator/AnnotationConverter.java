package app.bpartners.geojobs.service.annotator;

import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import org.springframework.core.convert.converter.Converter;

public class AnnotationConverter implements Converter<AnnotationBatch, MachineDetectedTile> {
  @Override
  public MachineDetectedTile convert(AnnotationBatch source) {
    return null;
  }
}
