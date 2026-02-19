package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.PROCESSING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CityJSONRequestServiceTest {
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  EventProducer eventProducerMock = mock();

  CityJSONRequestService subject =
      new CityJSONRequestService(cityJSONRequestRepositoryMock, eventProducerMock);

  @Test
  void throw_exception_when_already_processed_request() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.of(mock()));

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.processAddressRequest(requestIdentifier, List.of(), communityOwnerId));

    assertEquals(
        "Process request with id "
            + requestIdentifier
            + " can not be either updated or processed again",
        actual.getMessage());
  }

  @Test
  void produces_event_when_not_existing_request() {
    var requestIdentifier = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var someAddress = List.of("some address");
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(
            requestIdentifier, communityOwnerId))
        .thenReturn(Optional.empty());
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    var actual = subject.processAddressRequest(requestIdentifier, someAddress, communityOwnerId);

    assertEquals(
        CityJSONRequest.builder()
            .id(requestIdentifier)
            .status(PROCESSING)
            .cityJsons(null)
            .delimitations(null)
            .communityOwnerId(communityOwnerId)
            .creationDatetime(actual.getCreationDatetime())
            .build(),
        actual);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    ThreeDMultipleAddressRequested multipleAddressRequested =
        (ThreeDMultipleAddressRequested) listCaptor.getValue().getFirst();
    assertEquals(
        new ThreeDMultipleAddressRequested(requestIdentifier, someAddress),
        multipleAddressRequested);
  }
}
