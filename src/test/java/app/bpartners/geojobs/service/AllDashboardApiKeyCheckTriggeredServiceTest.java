package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.datastructure.ListGrouper;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.event.AllDashboardApiKeyCheckTriggeredService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AllDashboardApiKeyCheckTriggeredServiceTest {
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock =
      mock(CommunityAuthorizationRepository.class);
  EventProducer<DashboardApiKeyCheckTriggered> eventProducerMock = mock(EventProducer.class);
  ListGrouper<DashboardApiKeyCheckTriggered> listGrouper = new ListGrouper<>();

  AllDashboardApiKeyCheckTriggeredService subject;

  @Test
  void all_dashboard_key_check_trigger_dashboard_key_check() {
    List<CommunityAuthorization> communityAuthorizations =
        multipleCommunityAuthorization(mock(CommunityAuthorization.class));
    List<DashboardApiKeyCheckTriggered> childEvents = toChildEvents(communityAuthorizations);

    when(communityAuthorizationRepositoryMock.findAll()).thenReturn(communityAuthorizations);
    doNothing().when(eventProducerMock).accept(eq(childEvents));
    subject =
        new AllDashboardApiKeyCheckTriggeredService(
            communityAuthorizationRepositoryMock, eventProducerMock, listGrouper);

    subject.accept(new AllDashboardApiKeyCheckTriggered());

    verify(communityAuthorizationRepositoryMock, times(1)).findAll();
    verify(eventProducerMock, times(1)).accept(eq(childEvents));
  }

  @Test
  void all_dashboard_key_check_chunks_and_sends_gradually() {
    // GIVEN we have 25 community authorizations
    List<CommunityAuthorization> communityAuthorizations = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      CommunityAuthorization mockAuth = mock(CommunityAuthorization.class);
      when(mockAuth.getId()).thenReturn("id-" + i);
      when(mockAuth.getEmail()).thenReturn("user" + i + "@test.com");
      when(mockAuth.getDashboardApiKey()).thenReturn("key-" + i);
      communityAuthorizations.add(mockAuth);
    }

    when(communityAuthorizationRepositoryMock.findAll()).thenReturn(communityAuthorizations);

    subject =
        new AllDashboardApiKeyCheckTriggeredService(
            communityAuthorizationRepositoryMock, eventProducerMock, listGrouper);

    // WHEN the service accepts the event
    subject.accept(new AllDashboardApiKeyCheckTriggered());

    // THEN we verify the findAll is called once
    verify(communityAuthorizationRepositoryMock, times(1)).findAll();

    // AND we capture the arguments passed to eventProducer.accept
    ArgumentCaptor<List<DashboardApiKeyCheckTriggered>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(3)).accept(captor.capture());

    List<List<DashboardApiKeyCheckTriggered>> capturedBatches = captor.getAllValues();
    assertEquals(3, capturedBatches.size());

    // Verify first batch (size 10)
    assertEquals(10, capturedBatches.get(0).size());
    assertEquals("id-1", capturedBatches.get(0).get(0).getCommunityAuthorizationId());
    assertEquals("id-10", capturedBatches.get(0).get(9).getCommunityAuthorizationId());

    // Verify second batch (size 10)
    assertEquals(10, capturedBatches.get(1).size());
    assertEquals("id-11", capturedBatches.get(1).get(0).getCommunityAuthorizationId());
    assertEquals("id-20", capturedBatches.get(1).get(9).getCommunityAuthorizationId());

    // Verify third batch (size 5)
    assertEquals(5, capturedBatches.get(2).size());
    assertEquals("id-21", capturedBatches.get(2).get(0).getCommunityAuthorizationId());
    assertEquals("id-25", capturedBatches.get(2).get(4).getCommunityAuthorizationId());
  }

  private List<CommunityAuthorization> multipleCommunityAuthorization(
      CommunityAuthorization communityAuthorization) {
    return List.of(communityAuthorization, communityAuthorization, communityAuthorization);
  }

  private List<DashboardApiKeyCheckTriggered> toChildEvents(
      List<CommunityAuthorization> communityAuthorizations) {
    return communityAuthorizations.stream()
        .map(
            ca ->
                DashboardApiKeyCheckTriggered.builder()
                    .communityAuthorizationId(ca.getId())
                    .email(ca.getEmail())
                    .dashboardApiKey(ca.getDashboardApiKey())
                    .build())
        .toList();
  }
}
