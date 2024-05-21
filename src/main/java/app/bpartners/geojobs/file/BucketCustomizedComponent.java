package app.bpartners.geojobs.file;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
@AllArgsConstructor
// TODO: must be available from BucketComponent so update POJA BucketComponent
public class BucketCustomizedComponent {
  private final BucketConf bucketConf;

  public List<S3Object> listObjects(String bucketPath) {
    var s3Client = bucketConf.getS3Client();
    return s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketPath).build()).contents();
  }
}
