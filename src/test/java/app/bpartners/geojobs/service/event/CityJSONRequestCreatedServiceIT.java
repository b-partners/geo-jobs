package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_2;
import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_INSURANCE;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_METER;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSONThreedProcessor;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class CityJSONRequestCreatedServiceIT extends FacadeIT {
  @MockBean BucketComponent bucketComponentMock;
  @Autowired CityJSONRequestCreatedService subject;
  @MockBean CityJSONThreedProcessor threedProcessorMock;
  @MockBean private EventProducer eventProducerMock;
  @Autowired FeatureMapper featureMapper;
  @Autowired CityJSONRequestRepository cityJSONRequestRepository;
  @Autowired CommunityAuthorizationRepository communityAuthorizationRepository;

  private static final String COMMUNITY_OWNER_ID = randomUUID().toString();
  private static final String REQUEST_ID = randomUUID().toString();

  @BeforeEach()
  void setUp() {
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock());
    communityAuthorizationRepository.save(
        CommunityAuthorization.builder()
            .id(COMMUNITY_OWNER_ID)
            .name(randomUUID().toString())
            .email(randomUUID().toString())
            .role(ROLE_INSURANCE)
            .maxSurfaceUnit(SQUARE_METER)
            .detectableModels(List.of(TOITURE))
            .dashboardApiKey(randomUUID().toString())
            .build());
    cityJSONRequestRepository.save(request());
  }

  @AfterEach()
  void cleanUp() {
    cityJSONRequestRepository.deleteById(REQUEST_ID);
    communityAuthorizationRepository.deleteById(COMMUNITY_OWNER_ID);
  }

  @Test
  void generate_ok() {
    when(threedProcessorMock.apply(any())).thenReturn(List.of());

    subject.accept(
        CityJSONRequestCreated.builder()
            .requestId(REQUEST_ID)
            .communityOwnerId(COMMUNITY_OWNER_ID)
            .build());

    var actualRequest =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(REQUEST_ID, COMMUNITY_OWNER_ID)
            .orElseThrow();
    assertEquals(FINISHED, actualRequest.getStatus());
    verify(threedProcessorMock).apply(any());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(listCaptor.capture());
    var actualMonitoringTriggered =
        (ThreeDRequestMonitoringTriggered) listCaptor.getValue().getFirst();
    assertEquals(
        new ThreeDRequestMonitoringTriggered(REQUEST_ID, COMMUNITY_OWNER_ID),
        actualMonitoringTriggered);
    assertEquals(EVENT_STACK_2, actualMonitoringTriggered.getEventStack());
    assertEquals(
        Duration.ofMinutes(2), actualMonitoringTriggered.maxConsumerBackoffBetweenRetries());
    assertEquals(Duration.ofMinutes(5), actualMonitoringTriggered.maxConsumerDuration());
  }

  @Test
  void should_be_failed_when_threed_fails() {
    when(threedProcessorMock.apply(any())).thenThrow(RuntimeException.class);

    var request =
        CityJSONRequestCreated.builder()
            .requestId(REQUEST_ID)
            .communityOwnerId(COMMUNITY_OWNER_ID)
            .build();
    assertThrows(RuntimeException.class, () -> subject.accept(request));

    var actualRequest =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(REQUEST_ID, COMMUNITY_OWNER_ID)
            .orElseThrow();
    assertEquals(FAILED, actualRequest.getStatus());
  }

  private CityJSONRequest request() {
    return CityJSONRequest.builder()
        .id(REQUEST_ID)
        .communityOwnerId(COMMUNITY_OWNER_ID)
        .delimitations(List.of(roofFeature()))
        .delimitationObjectType(BUILDING_ROOF_SEGMENT_FACE)
        .creationDatetime(now())
        .build();
  }

  private Feature roofFeature() {
    var restFeature = featureMapper.toRest(roofPolygon(), 20, randomUUID().toString());
    return toDomainFeature(restFeature);
  }

  private Polygon roofPolygon() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }
}
