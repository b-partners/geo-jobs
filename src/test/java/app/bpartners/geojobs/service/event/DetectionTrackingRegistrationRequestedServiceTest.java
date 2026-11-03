package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionTrackingRegistrationRequested;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DetectionTrackingRegister;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import app.bpartners.geojobs.service.dashboard.component.DetectionInitiator;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionTrackingRegistrationRequestedServiceTest {
  AnalysisProjectApi analysisProjectApiMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  DetectionTrackingRegister detectionTrackingRegister =
      new DetectionTrackingRegister(
          analysisProjectApiMock, communityAuthorizationRepositoryMock, detectionRepositoryMock);
  DetectionTrackingRegistrationRequestedService subject =
      new DetectionTrackingRegistrationRequestedService(
          detectionTrackingRegister, detectionRepositoryMock);

  final String apiKey = "apiKey";

  @BeforeEach
  void setUp() {
    when(analysisProjectApiMock.register2d(eq(apiKey), any(), any()))
        .thenReturn(mock(AnalysisProject.class));
  }

  @Test
  void register_detection_ok() {
    var detectionIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var zoneName = "detection zone name";
    var emailReceiver = "detection email receiver";
    var endToEndId = "detection end to end id";
    var detectionBuilderMock = mock(Detection.DetectionBuilder.class);

    var detectionMock = mock(Detection.class);
    var detectionMockWithRegistrationDatetime = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getZoneName()).thenReturn(zoneName);
    when(detectionMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionMock.getEndToEndId()).thenReturn(endToEndId);
    when(detectionMock.getCommunityOwnerId()).thenReturn(communityOwnerId);
    when(detectionBuilderMock.dashboardRegistrationDatetime(any()))
        .thenReturn(detectionBuilderMock);
    when(detectionMock.toBuilder()).thenReturn(detectionBuilderMock);
    when(detectionBuilderMock.build()).thenReturn(detectionMockWithRegistrationDatetime);

    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(detectionRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(CommunityAuthorization.builder().dashboardApiKey(apiKey).build()));

    assertDoesNotThrow(
        () -> subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier)));

    var projectKeyCaptor = ArgumentCaptor.forClass(String.class);
    var registerCaptor = ArgumentCaptor.forClass(Register2d.class);
    verify(analysisProjectApiMock, only())
        .register2d(eq(apiKey), projectKeyCaptor.capture(), registerCaptor.capture());
    var expectedRegister2d =
        getExpectedRegister2d(zoneName, emailReceiver, endToEndId, registerCaptor.getValue());
    verify(detectionRepositoryMock).save(detectionMockWithRegistrationDatetime);

    assertEquals(expectedRegister2d, registerCaptor.getValue());
    assertEquals("detection:" + detectionIdentifier, projectKeyCaptor.getValue());
  }

  private @NotNull Register2d getExpectedRegister2d(
      String zoneName, String emailReceiver, String endToEndId, Register2d actual) {
    return new Register2d(
        endToEndId,
        zoneName,
        "non supportée",
        actual.creationDatetime(),
        new DetectionInitiator("non supporté", emailReceiver, "non supporté"));
  }

  @Test
  void register_detection_fail_on_api_failure() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getCommunityOwnerId()).thenReturn(randomUUID().toString());
    reset(analysisProjectApiMock);
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(communityAuthorizationRepositoryMock.findById(any()))
        .thenReturn(Optional.of(CommunityAuthorization.builder().apiKey(apiKey).build()));
    when(analysisProjectApiMock.register2d(any(), any(), any())).thenThrow(new RuntimeException());

    assertThrows(
        RuntimeException.class,
        () -> subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier)));
  }

  @Test
  void register_detection_fail_on_api_key_not_found() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getCommunityOwnerId()).thenReturn(randomUUID().toString());
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));
    when(communityAuthorizationRepositoryMock.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        RuntimeException.class,
        () -> subject.accept(new DetectionTrackingRegistrationRequested(detectionIdentifier)));
  }
}
