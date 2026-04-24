package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.model.RevokeApiKey;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class RevokeApiKeyValidator implements Consumer<RevokeApiKey> {
  @Override
  public void accept(RevokeApiKey revokeApiKey) {
    var key = revokeApiKey.getKeyValue();

    if (key == null || key.toString().isBlank()) {
      throw new BadRequestException("keyValue is mandatory.");
    }
  }
}
