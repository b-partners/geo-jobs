package app.bpartners.geojobs.file.bucket;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Dedicated bucket for Wallonia LIDAR point clouds. Kept separate from the project's main bucket
 * ({@link BucketConf}) and from its per-environment naming, since the same LAZ files are reused
 * as-is between preprod and prod (the source data does not vary per environment).
 */
@Configuration
public class WalloniaBucketConf {
  @Getter private final String bucketName;
  @Getter private final S3Presigner s3Presigner;

  public WalloniaBucketConf(
      @Value("eu-west-3") String regionString,
      @Value("${wallonia.lidar.bucket.name}") String bucketName) {
    this.bucketName = bucketName;
    this.s3Presigner = S3Presigner.builder().region(Region.of(regionString)).build();
  }
}
