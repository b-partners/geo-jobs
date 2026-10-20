package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.USER_DEFINED_DELIMITATION;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.model.Lrg;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class CityJSONThreedProcessorTest {
  private final BucketComponent bucketComponentMock = mock();
  private final ThreedApiClient apiClientMock = mock();
  private final CityJsonTextureComputer textureComputerMock = mock();
  private final CityJSONThreedProcessor.CityJSONDownloader cityJSONDownloaderMock = mock();
  private final CommunityAuthorizationRepository caRepositoryMock = mock();

  private final CityJSONThreedProcessor subject =
      new CityJSONThreedProcessor(
          new FileWriter(new ObjectMapper(), new ExtensionGuesser()),
          bucketComponentMock,
          textureComputerMock,
          apiClientMock,
          cityJSONDownloaderMock,
          caRepositoryMock);

  @BeforeEach
  void setup() {
    FileHash uploadedFileHash = mock();
    URL presignedUrl = mock();
    Lrg generatedLrg = mock();
    Answer<File> textureAnswer = mock();
    File downloadedFile = mock();

    when(bucketComponentMock.upload(any(), any())).thenReturn(uploadedFileHash);
    when(bucketComponentMock.presign(any(), any())).thenReturn(presignedUrl);
    when(apiClientMock.generate(any(), any())).thenReturn(generatedLrg);
    when(textureComputerMock.applyTexture(any(), any())).then(textureAnswer);
    when(cityJSONDownloaderMock.download(any())).thenReturn(downloadedFile);

    var communityApiKey =
        CommunityAuthorizationApiKey.builder().keyValue(randomUUID().toString()).build();
    var community = CommunityAuthorization.builder().apiKeys(List.of(communityApiKey)).build();
    when(caRepositoryMock.findById(any())).thenReturn(Optional.of(community));
  }

  @Test
  void should_call_internal_processor_when_roofer_throws() {
    var actual =
        subject.apply(
            CityJSONRequest.builder()
                .id(randomUUID().toString())
                .delimitationType(USER_DEFINED_DELIMITATION)
                .delimitationObjectType(BUILDING_ROOF)
                .delimitations(List.of())
                .build());

    assertNotNull(actual);
  }
}
