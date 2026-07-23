package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.FAILED;
import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.SUCCEEDED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.GeoCodingJobCreated;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.GeoCodingJobRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJob;
import app.bpartners.geojobs.service.ExcelAddressConverter;
import app.bpartners.geojobs.service.GeoCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GeoCodingJobCreatedServiceTest {
  GeoCodingJobRepository repositoryMock = mock();
  ExcelAddressConverter excelAddressConverterMock = mock();
  BucketComponent bucketComponentMock = mock();
  GeoCodeService geoCodeServiceMock = mock();
  ObjectMapper objectMapper = new ObjectMapper();

  GeoCodingJobCreatedService subject =
      new GeoCodingJobCreatedService(
          repositoryMock,
          excelAddressConverterMock,
          bucketComponentMock,
          geoCodeServiceMock,
          objectMapper);

  private GeoCodingJob geoCodingJob(String jobId, Integer sheetIndex) {
    return GeoCodingJob.builder()
        .id(jobId)
        .endToEndId(randomUUID().toString())
        .fileKey("geocoding/" + jobId + ".xlsx")
        .sheetIndex(sheetIndex)
        .status(PROCESSING)
        .build();
  }

  @SneakyThrows
  private File downloadedExcelFile() {
    return File.createTempFile("downloaded-addresses-", ".xlsx");
  }

  @SneakyThrows
  @Test
  void process_geo_coding_job_with_provided_sheet_index_ok() {
    var jobId = randomUUID().toString();
    var sheetIndex = 3;
    var job = geoCodingJob(jobId, sheetIndex);
    var downloadedExcelFile = downloadedExcelFile();
    when(repositoryMock.findById(jobId)).thenReturn(Optional.of(job));
    when(bucketComponentMock.download(job.getFileKey())).thenReturn(downloadedExcelFile);
    when(excelAddressConverterMock.apply(downloadedExcelFile, sheetIndex))
        .thenReturn(List.of("25 avenue Mozart, 75001, Paris, France"));
    when(geoCodeServiceMock.geocode(any(String.class))).thenReturn(Feature.builder().build());

    subject.accept(new GeoCodingJobCreated(jobId));

    var sheetIndexCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(excelAddressConverterMock).apply(any(File.class), sheetIndexCaptor.capture());
    assertEquals(sheetIndex, sheetIndexCaptor.getValue());
    var savedJobCaptor = ArgumentCaptor.forClass(GeoCodingJob.class);
    verify(repositoryMock).save(savedJobCaptor.capture());
    var savedJob = savedJobCaptor.getValue();
    assertEquals(SUCCEEDED, savedJob.getStatus());
    assertEquals(sheetIndex, savedJob.getSheetIndex());
    assertTrue(savedJob.getGeoJsonKey().startsWith("geocoding/" + jobId + "/geoJson/"));
  }

  @SneakyThrows
  @Test
  void process_geo_coding_job_without_sheet_index_ok() {
    var jobId = randomUUID().toString();
    var job = geoCodingJob(jobId, null);
    var downloadedExcelFile = downloadedExcelFile();
    when(repositoryMock.findById(jobId)).thenReturn(Optional.of(job));
    when(bucketComponentMock.download(job.getFileKey())).thenReturn(downloadedExcelFile);
    when(excelAddressConverterMock.apply(downloadedExcelFile, null)).thenReturn(List.of());

    subject.accept(new GeoCodingJobCreated(jobId));

    var sheetIndexCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(excelAddressConverterMock).apply(any(File.class), sheetIndexCaptor.capture());
    assertNull(sheetIndexCaptor.getValue());
    var savedJobCaptor = ArgumentCaptor.forClass(GeoCodingJob.class);
    verify(repositoryMock).save(savedJobCaptor.capture());
    assertEquals(SUCCEEDED, savedJobCaptor.getValue().getStatus());
    assertNull(savedJobCaptor.getValue().getSheetIndex());
  }

  @Test
  void process_geo_coding_job_of_unreadable_sheet_index_ko() {
    var jobId = randomUUID().toString();
    var sheetIndex = 4;
    var job = geoCodingJob(jobId, sheetIndex);
    var downloadedExcelFile = downloadedExcelFile();
    when(repositoryMock.findById(jobId)).thenReturn(Optional.of(job));
    when(bucketComponentMock.download(job.getFileKey())).thenReturn(downloadedExcelFile);
    when(excelAddressConverterMock.apply(downloadedExcelFile, sheetIndex))
        .thenThrow(new IllegalArgumentException("Sheet index (3) is out of range (0..2)"));

    subject.accept(new GeoCodingJobCreated(jobId));

    var savedJobCaptor = ArgumentCaptor.forClass(GeoCodingJob.class);
    verify(repositoryMock).save(savedJobCaptor.capture());
    var savedJob = savedJobCaptor.getValue();
    assertEquals(FAILED, savedJob.getStatus());
    assertNull(savedJob.getGeoJsonKey());
    assertEquals(sheetIndex, savedJob.getSheetIndex());
  }

  @Test
  void process_unknown_geo_coding_job_ko() {
    var jobId = randomUUID().toString();
    when(repositoryMock.findById(jobId)).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class, () -> subject.accept(new GeoCodingJobCreated(jobId)));

    verifyNoInteractions(excelAddressConverterMock);
    verifyNoInteractions(bucketComponentMock);
  }
}
