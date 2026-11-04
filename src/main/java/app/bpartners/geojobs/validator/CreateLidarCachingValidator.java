package app.bpartners.geojobs.validator;

import app.bpartners.geojobs.endpoint.rest.model.CreateLidarCaching;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateLidarCachingValidator implements Consumer<CreateLidarCaching> {
  @Override
  public void accept(CreateLidarCaching createLidarCaching) {
    if (createLidarCaching == null
        || createLidarCaching.getLatitude() == null
        || createLidarCaching.getLongitude() == null) {
      throw new BadRequestException("latitude and longitude are mandatory");
    }
    var latitude = createLidarCaching.getLatitude();
    var longitude = createLidarCaching.getLongitude();
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      throw new BadRequestException(
          "WGS84 latitude must be in [-90, 90] and longitude in [-180, 180], but were "
              + latitude
              + ", "
              + longitude);
    }
  }
}
