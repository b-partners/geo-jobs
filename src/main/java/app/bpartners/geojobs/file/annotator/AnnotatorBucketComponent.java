package app.bpartners.geojobs.file.annotator;

import app.bpartners.geojobs.file.AbstractBucketComponent;
import org.springframework.stereotype.Component;

@Component
public class AnnotatorBucketComponent extends AbstractBucketComponent {
  public AnnotatorBucketComponent(AnnotatorBucketConfiguration bucketConf) {
    super(bucketConf);
  }
}
