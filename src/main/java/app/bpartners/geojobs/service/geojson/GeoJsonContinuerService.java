package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.GeoJsonContinuationRepository;
import java.io.File;
import java.time.Duration;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GeoJsonContinuerService {
  private final BucketComponent bucketComponent;
  private final EventProducer<GeoJsonContinuerIsCompleted> eventProducer;
  private final GeoJsonContinuationRepository repository;

  public String generatePresignedUrl(String id, File file) {
    var geoJsonToContinue = repository.findById(id);
    var geoJsonFileKey = "continuations/geojson/" + id + ".geojson";
    if (geoJsonToContinue.isPresent()) {
      return bucketComponent.presign(geoJsonFileKey);
    }
    bucketComponent.upload(file, geoJsonFileKey);
    eventProducer.accept(
        List.of(GeoJsonContinuerIsCompleted.builder().id(id).fileKey(geoJsonFileKey).build()));
    var geoJson =
        repository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("geojson with id " + id + " not found"));
    return bucketComponent.presign(geoJson.getFileyKey(), Duration.ofHours(1L)).toString();
  }
}
