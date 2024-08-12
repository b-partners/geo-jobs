package app.bpartners.geojobs.file.bucket;

import app.bpartners.geojobs.file.FileWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

@Getter
@Component
public class CustomBucketComponent {
  private final BucketConf bucketConf;
  private final FileWriter fileWriter;

  public CustomBucketComponent(BucketConf bucketConf, FileWriter fileWriter) {
    this.bucketConf = bucketConf;
    this.fileWriter = fileWriter;
  }

  public List<S3Object> listObjects(String bucketName) {
    var s3Client = bucketConf.getS3Client();
    return s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build()).contents();
  }

  public List<S3Object> listObjects(String bucketName, String prefix) {
    var s3Client = bucketConf.getS3Client();
    String continuationToken = null;
    List<S3Object> allS3Objects = new ArrayList<>();
    do {
      ListObjectsV2Request listObjectsV2Request =
          ListObjectsV2Request.builder()
              .bucket(bucketName)
              .prefix(prefix)
              .continuationToken(continuationToken)
              .build();
      ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
      allS3Objects.addAll(listObjectsV2Response.contents());
      continuationToken = listObjectsV2Response.nextContinuationToken();
    } while (continuationToken != null);

    return allS3Objects;
  }

  @SneakyThrows
  public File download(String bucketName, String bucketKey) {
    var destination =
        fileWriter
            .createTempFileSecurely(prefixFromBucketKey(bucketKey), suffixFromBucketKey(bucketKey))
            .toFile();
    FileDownload download =
        bucketConf
            .getS3TransferManager()
            .downloadFile(
                DownloadFileRequest.builder()
                    .getObjectRequest(
                        GetObjectRequest.builder().bucket(bucketName).key(bucketKey).build())
                    .destination(destination)
                    .build());
    download.completionFuture().join();
    return destination;
  }

  private String prefixFromBucketKey(String bucketKey) {
    return lastNameSplitByDot(bucketKey)[0];
  }

  private String suffixFromBucketKey(String bucketKey) {
    var splitByDot = lastNameSplitByDot(bucketKey);
    return splitByDot.length == 1 ? "" : splitByDot[splitByDot.length - 1];
  }

  private String[] lastNameSplitByDot(String bucketKey) {
    var splitByDash = bucketKey.split("/");
    var lastName = splitByDash[splitByDash.length - 1];
    return lastName.split("\\.");
  }
}
