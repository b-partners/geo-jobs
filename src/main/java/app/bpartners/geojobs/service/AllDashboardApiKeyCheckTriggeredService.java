package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AllDashboardApiKeyCheckTriggeredService
    implements Consumer<AllDashboardApiKeyCheckTriggered> {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AllDashboardApiKeyCheckTriggered event) {
    PageRequest pageRequest = PageRequest.of(0, 100);

    Page<CommunityAuthorization> page;
    do {
      page = communityAuthorizationRepository.findAll(pageRequest);
      eventProducer.accept(page.stream().map(this::toTypedEvent).toList());
      pageRequest = pageRequest.next();
    } while (page.hasNext());
  }

  private DashboardApiKeyCheckTriggered toTypedEvent(CommunityAuthorization authorization) {
    return DashboardApiKeyCheckTriggered.builder().communityAuthorization(authorization).build();
  }
}
