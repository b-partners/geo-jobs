package app.bpartners.geojobs.file.self;

import app.bpartners.geojobs.file.AbstractBucketComponent;
import org.springframework.stereotype.Component;

@Component
public class BucketComponent extends AbstractBucketComponent {
  public BucketComponent(BucketConfiguration bucketConf) {
    super(bucketConf);
  }
}
