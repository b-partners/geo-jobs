package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.GeoJsonContinuationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
public class GeoJsonContinuerControllerIT extends FacadeIT {
  @Autowired private GeoJsonContinuerController subject;
  @MockBean private GeoJsonContinuationRepository repository;
  @MockBean private EventProducer eventProducer;
  @MockBean private BucketComponent bucketComponent;
}
