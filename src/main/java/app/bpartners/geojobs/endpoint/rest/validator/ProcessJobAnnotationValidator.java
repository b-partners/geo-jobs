package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.model.ProcessJobAnnotation;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ProcessJobAnnotationValidator implements Consumer<ProcessJobAnnotation> {
  @Override
  public void accept(ProcessJobAnnotation processJobAnnotation) {
    var builder = new StringBuilder();
    Double minimumConfidence = processJobAnnotation.getMinimumConfidence();
    if (minimumConfidence == null) {
      builder.append("MinimumConfidence is mandatory");
    } else if (minimumConfidence > 1 || minimumConfidence < 0) {
      builder
          .append("MinimumConfidence must be between 0 and 1, otherwise its provided value is ")
          .append(minimumConfidence);
    }
    if (!builder.isEmpty()) {
      throw new BadRequestException(builder.toString());
    }
  }
}
