package app.bpartners.geojobs.service.annotator;

import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import org.springframework.core.convert.converter.Converter;

public class AnnotationConverter implements Converter<AnnotationBatch, DetectedTile> {
  @Override
  public DetectedTile convert(AnnotationBatch source) {
    return null;
  }
}
