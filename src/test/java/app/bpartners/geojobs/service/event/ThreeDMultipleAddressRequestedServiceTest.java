package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FAILED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDMultipleAddressRequested;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ThreeDMultipleAddressRequestedServiceTest {
  FeatureAddressConverter featureAddressConverterMock = mock();
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  ThreeDMultipleAddressRequestedService subject =
      new ThreeDMultipleAddressRequestedService(
          featureAddressConverterMock, cityJSONRequestRepositoryMock, eventProducerMock);

  @Test
  void do_not_produces_event_computing_when_address_conversion_fails() {
    var requestIdentifier = randomUUID().toString();
    var someAddress = "random " + randomUUID();
    var request = CityJSONRequest.builder().build();
    when(cityJSONRequestRepositoryMock.findById(requestIdentifier))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(someAddress, BUILDING))
        .thenThrow(new ApiException(SERVER_EXCEPTION, "Unable to convert address to feature"));
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .addresses(List.of(someAddress))
                    .build()));
    var savedFailedRequestCaptor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedFailedRequestCaptor.capture());
    verify(eventProducerMock, never()).accept(any());
    assertEquals(
        CityJSONRequest.builder().status(FAILED).build(), savedFailedRequestCaptor.getValue());
  }

  @Test
  void throws_exception_when_request_not_find_from_db() {
    var requestIdentifier = randomUUID().toString();
    when(cityJSONRequestRepositoryMock.findById(requestIdentifier)).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            NoSuchElementException.class,
            () ->
                subject.accept(
                    ThreeDMultipleAddressRequested.builder()
                        .requestIdentifier(requestIdentifier)
                        .addresses(null)
                        .build()));

    assertEquals("No value present", actual.getMessage());
  }

  @Test
  void produces_event_computing_when_address_conversion_succeeds() {
    var requestIdentifier = randomUUID().toString();
    var someAddress = "random " + randomUUID();
    var request = CityJSONRequest.builder().id(requestIdentifier).build();
    var convertedAddressFeature = mock(Feature.class);
    var expectedCityJSONRequestSaved =
        CityJSONRequest.builder()
            .id(requestIdentifier)
            .delimitations(List.of(convertedAddressFeature))
            .build();
    when(cityJSONRequestRepositoryMock.findById(requestIdentifier))
        .thenReturn(Optional.of(request));
    when(featureAddressConverterMock.apply(someAddress, BUILDING))
        .thenReturn(convertedAddressFeature);
    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () ->
            subject.accept(
                ThreeDMultipleAddressRequested.builder()
                    .requestIdentifier(requestIdentifier)
                    .addresses(List.of(someAddress))
                    .build()));

    var savedRequestWithDelimitation = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(savedRequestWithDelimitation.capture());
    assertEquals(expectedCityJSONRequestSaved, savedRequestWithDelimitation.getValue());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var actualRequestCreatedEvent = (CityJSONRequestCreated) listCaptor.getValue().getFirst();
    assertEquals(
        CityJSONRequestCreated.builder().requestId(requestIdentifier).build(),
        actualRequestCreatedEvent);
  }
}
