package app.bpartners.geojobs.file;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public interface BucketConfiguration {
  String getBucketName();

  S3TransferManager getS3TransferManager();

  S3Presigner getS3Presigner();
}
