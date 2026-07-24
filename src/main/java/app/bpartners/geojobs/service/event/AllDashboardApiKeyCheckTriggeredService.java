package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.datastructure.ListGrouper;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllDashboardApiKeyCheckTriggeredService
    implements Consumer<AllDashboardApiKeyCheckTriggered> {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final EventProducer<DashboardApiKeyCheckTriggered> eventProducer;
  private final ListGrouper<DashboardApiKeyCheckTriggered> listGrouper;

  private static final int CHUNK_SIZE = 25;
  private static final long DELAY_BETWEEN_CHUNKS_MS = 1000L;

  @Override
  public void accept(AllDashboardApiKeyCheckTriggered event) {
    List<CommunityAuthorization> allCommunityAuthorizations =
        communityAuthorizationRepository.findAllByIntegrationTestUsageFalse();

    List<DashboardApiKeyCheckTriggered> allEvents =
        allCommunityAuthorizations.stream().map(this::toTypedEvent).toList();

    List<List<DashboardApiKeyCheckTriggered>> chunkedEvents =
        listGrouper.apply(allEvents, CHUNK_SIZE);

    for (int i = 0; i < chunkedEvents.size(); i++) {
      List<DashboardApiKeyCheckTriggered> chunk = chunkedEvents.get(i);
      log.info(
          "Sending AllDashboardApiKeyCheckTriggered chunk {}/{} containing {} events",
          i + 1,
          chunkedEvents.size(),
          chunk.size());
      eventProducer.accept(chunk);

      if (i < chunkedEvents.size() - 1) {
        try {
          Thread.sleep(DELAY_BETWEEN_CHUNKS_MS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Sending of AllDashboardApiKeyCheckTriggered chunk was interrupted", e);
          break;
        }
      }
    }
  }

  private DashboardApiKeyCheckTriggered toTypedEvent(CommunityAuthorization authorization) {
    return DashboardApiKeyCheckTriggered.builder()
        .communityAuthorizationId(authorization.getId())
        .email(authorization.getEmail())
        .dashboardApiKey(authorization.getDashboardApiKey())
        .build();
  }
}
