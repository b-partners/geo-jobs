package app.bpartners.geojobs.file.bucket;

import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@AllArgsConstructor
public class WalloniaBucketComponent {
  private static final Duration DEFAULT_PRE_SIGNED_URL_DURATION = Duration.ofHours(1L);

  private final WalloniaBucketConf bucketConf;

  public String getBucketName() {
    return bucketConf.getBucketName();
  }

  public String presign(String bucketKey) {
    var getObjectRequest =
        GetObjectRequest.builder().bucket(bucketConf.getBucketName()).key(bucketKey).build();
    var presignedRequest =
        bucketConf
            .getS3Presigner()
            .presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(DEFAULT_PRE_SIGNED_URL_DURATION)
                    .getObjectRequest(getObjectRequest)
                    .build());
    return presignedRequest.url().toString();
  }
}
