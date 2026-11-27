package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POINT;
import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.CityJSONController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.cityjson.CityJSONRequestMapper;
import app.bpartners.geojobs.endpoint.rest.model.AddressFullText;
import app.bpartners.geojobs.endpoint.rest.model.CityJSONRequest;
import app.bpartners.geojobs.endpoint.rest.model.CreateCityJSONRequest;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetectionExportRequest;
import app.bpartners.geojobs.endpoint.rest.model.CreateRoofIntersections;
import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.endpoint.rest.model.HealthStatus;
import app.bpartners.geojobs.endpoint.rest.model.LidarProcessorType;
import app.bpartners.geojobs.endpoint.rest.model.RoofIntersections;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDAddressesRequest;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDRequest;
import app.bpartners.geojobs.endpoint.rest.model.ThreeDResponseStatus;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CityJSONRequestValidator;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.endpoint.rest.validator.CreateDetectionExportRequestValidator;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CityJSONRequestExportService;
import app.bpartners.geojobs.service.CityJSONRequestService;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import app.bpartners.geojobs.service.RoofIntersectionsService;
import app.bpartners.geojobs.service.ThreeDCreditGuard;
import app.bpartners.geojobs.validator.CreateCityJSONRequestValidator;
import app.bpartners.geojobs.validator.CreateRoofIntersectionsValidator;
import app.bpartners.geojobs.validator.ThreeDAddressesRequestValidator;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CityJSONControllerTest {
  private static final String API_KEY = "apiKey";
  private static final String COMMUNITY_ID = "communityId";
  private static final String REQUEST_ID = "requestId";

  CityJSONRequestMapper cityJSONRequestMapperMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  AuthProvider authProviderMock = mock();
  CityJSONRequestService cityJSONRequestServiceMock = mock();
  CityJSONRequestValidator cityJSONRequestValidatorMock = mock();
  CreateCityJSONRequestValidator createCityJSONRequestValidatorMock = mock();
  ThreeDAddressesRequestValidator threeDAddressesRequestValidatorMock = mock();
  FeatureAddressConverter featureAddressConverterMock = mock();
  CreateRoofIntersectionsValidator createRoofIntersectionsValidatorMock = mock();
  RoofIntersectionsService roofIntersectionsServiceMock = mock();
  ThreeDCreditGuard threeDCreditGuardMock = mock();
  CityJSONRequestExportService cityJSONRequestExportServiceMock = mock();
  CreateDetectionExportRequestValidator createDetectionExportRequestValidatorMock = mock();

  CityJSONController subject =
      new CityJSONController(
          cityJSONRequestMapperMock,
          communityAuthorizationRepositoryMock,
          authProviderMock,
          cityJSONRequestServiceMock,
          cityJSONRequestValidatorMock,
          createCityJSONRequestValidatorMock,
          threeDAddressesRequestValidatorMock,
          featureAddressConverterMock,
          createRoofIntersectionsValidatorMock,
          roofIntersectionsServiceMock,
          threeDCreditGuardMock,
          cityJSONRequestExportServiceMock,
          createDetectionExportRequestValidatorMock);

  app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest domainRequest =
      mock(app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest.class);
  ThreeDResponseStatus restResponse = new ThreeDResponseStatus();

  @BeforeEach
  void setUp() {
    var principalMock = mock(Principal.class);
    when(principalMock.getPassword()).thenReturn(API_KEY);
    when(authProviderMock.getPrincipal()).thenReturn(principalMock);
    when(communityAuthorizationRepositoryMock.findByApiKey(API_KEY))
        .thenReturn(Optional.of(CommunityAuthorization.builder().id(COMMUNITY_ID).build()));
    when(cityJSONRequestMapperMock.createToDomain(any(), any(), any())).thenReturn(domainRequest);
    when(cityJSONRequestMapperMock.toRestThreeDResponseStatus(any())).thenReturn(restResponse);
  }

  @Test
  void export_three_d_requests_validates_exports_and_describes_the_export() {
    var from = LocalDate.parse("2026-01-01");
    var to = LocalDate.parse("2026-02-01");
    var statuses = List.of(HealthStatus.SUCCEEDED);
    var attributes = List.of(DetectionExportAttribute.values()[0]);
    var exportRequest =
        new CreateDetectionExportRequest()
            .from(from)
            .to(to)
            .detectionStatuses(statuses)
            .additionalExportedAttributes(attributes);
    when(cityJSONRequestExportServiceMock.requestExport(
            COMMUNITY_ID, "exportId", from, to, attributes, statuses))
        .thenReturn("https://export.url");

    var actual = subject.exportThreeDRequests(COMMUNITY_ID, "exportId", exportRequest);

    verify(createDetectionExportRequestValidatorMock).accept(exportRequest);
    assertEquals("exportId", actual.getId());
    assertEquals(from, actual.getFrom());
    assertEquals(to, actual.getTo());
    assertEquals(statuses, actual.getDetectionStatuses());
    assertEquals(attributes, actual.getAdditionalExportedAttributes());
    assertEquals("https://export.url", actual.getFileUrl());
  }

  @Test
  void get_requested_3d_file_reads_the_request_of_the_community() {
    when(cityJSONRequestServiceMock.getByIdAndCommunityOwnerId(REQUEST_ID, COMMUNITY_ID))
        .thenReturn(domainRequest);

    var actual = subject.getRequested3DFileById(REQUEST_ID);

    assertSame(restResponse, actual);
    verify(cityJSONRequestMapperMock).toRestThreeDResponseStatus(domainRequest);
  }

  @Test
  void request_3d_file_on_delimitations_validates_guards_credits_and_processes() {
    var threeDRequest = new ThreeDRequest();
    when(cityJSONRequestServiceMock.process(domainRequest)).thenReturn(domainRequest);

    var actual = subject.request3DFileOnDelimitations(threeDRequest, REQUEST_ID);

    assertSame(restResponse, actual);
    var order =
        inOrder(
            createCityJSONRequestValidatorMock,
            cityJSONRequestValidatorMock,
            threeDCreditGuardMock,
            cityJSONRequestServiceMock);
    order.verify(createCityJSONRequestValidatorMock).accept(threeDRequest);
    order.verify(cityJSONRequestValidatorMock).accept(REQUEST_ID, COMMUNITY_ID);
    order.verify(threeDCreditGuardMock).accept(domainRequest);
    order.verify(cityJSONRequestServiceMock).process(domainRequest);
  }

  @Test
  void request_3d_file_on_delimitations_sync_validates_without_async_and_processes_synchronously() {
    var threeDRequest = new ThreeDRequest();
    when(cityJSONRequestServiceMock.processSync(domainRequest)).thenReturn(domainRequest);

    var actual = subject.request3DFileOnDelimitationsSync(threeDRequest, REQUEST_ID);

    assertSame(restResponse, actual);
    verify(createCityJSONRequestValidatorMock).accept(threeDRequest, false);
    verify(cityJSONRequestValidatorMock).accept(REQUEST_ID, COMMUNITY_ID);
    verify(threeDCreditGuardMock).accept(domainRequest);
    verify(cityJSONRequestServiceMock).processSync(domainRequest);
  }

  @Test
  void request_3d_file_on_one_address_converts_it_to_a_building_delimitation() {
    var addressesRequest =
        new ThreeDAddressesRequest()
            .addresses(List.of(new AddressFullText().fullText("1 rue de Paris")));
    var building =
        Feature.builder()
            .geometry(
                Feature.FeatureGeometry.builder()
                    .geometryType(POINT)
                    .actualInstanceStringValue("{\"type\":\"Point\",\"coordinates\":[2.35,48.85]}")
                    .build())
            .build();
    when(featureAddressConverterMock.apply("1 rue de Paris", BUILDING)).thenReturn(building);
    when(cityJSONRequestServiceMock.process(domainRequest)).thenReturn(domainRequest);

    var actual = subject.request3DFileOnAddresses(addressesRequest, REQUEST_ID);

    assertSame(restResponse, actual);
    verify(threeDAddressesRequestValidatorMock).accept(addressesRequest);
    verify(cityJSONRequestValidatorMock).accept(REQUEST_ID, COMMUNITY_ID);
    verify(threeDCreditGuardMock).accept(domainRequest);
    verify(cityJSONRequestMapperMock)
        .createToDomain(
            eq(REQUEST_ID),
            org.mockito.ArgumentMatchers.argThat(
                (ThreeDRequest request) -> request.getDelimitations().size() == 1),
            eq(COMMUNITY_ID));
  }

  @Test
  void request_3d_file_on_several_addresses_processes_them_as_an_address_request() {
    var addressesRequest =
        new ThreeDAddressesRequest()
            .lidarProcessorType(LidarProcessorType.SAFE_MODE)
            .addresses(
                List.of(
                    new AddressFullText().fullText("1 rue de Paris"),
                    new AddressFullText().fullText("2 rue de Lyon")));
    when(cityJSONRequestServiceMock.processAddressRequest(
            REQUEST_ID,
            List.of("1 rue de Paris", "2 rue de Lyon"),
            COMMUNITY_ID,
            app.bpartners.geojobs.model.lidar.LidarProcessorType.SAFE_MODE))
        .thenReturn(domainRequest);

    var actual = subject.request3DFileOnAddresses(addressesRequest, REQUEST_ID);

    assertSame(restResponse, actual);
    verify(cityJSONRequestMapperMock).toRestThreeDResponseStatus(domainRequest);
  }

  @Test
  void request_3d_file_on_several_addresses_without_lidar_processor_uses_the_default_one() {
    var addressesRequest =
        new ThreeDAddressesRequest()
            .addresses(
                List.of(
                    new AddressFullText().fullText("1 rue de Paris"),
                    new AddressFullText().fullText("2 rue de Lyon")));
    when(cityJSONRequestServiceMock.processAddressRequest(any(), any(), any(), any()))
        .thenReturn(domainRequest);

    subject.request3DFileOnAddresses(addressesRequest, REQUEST_ID);

    verify(cityJSONRequestServiceMock)
        .processAddressRequest(
            REQUEST_ID, List.of("1 rue de Paris", "2 rue de Lyon"), COMMUNITY_ID, null);
  }

  @Test
  void put_roof_intersections_validates_then_computes() {
    var createRoofIntersections = new CreateRoofIntersections();
    var intersections = new RoofIntersections();
    when(roofIntersectionsServiceMock.compute("id", createRoofIntersections))
        .thenReturn(intersections);

    var actual = subject.putRoofIntersections(createRoofIntersections, "id");

    assertSame(intersections, actual);
    verify(createRoofIntersectionsValidatorMock).accept(createRoofIntersections);
  }

  @Test
  void process_deprecated_city_json_request_uses_the_deprecated_dto_mapping() {
    var createRequest = new CreateCityJSONRequest();
    var restRequest = new CityJSONRequest();
    when(cityJSONRequestMapperMock.createToDomainFromDeprecatedDTO(
            REQUEST_ID, createRequest, COMMUNITY_ID))
        .thenReturn(domainRequest);
    when(cityJSONRequestServiceMock.oldProcess(domainRequest)).thenReturn(domainRequest);
    when(cityJSONRequestMapperMock.toRest(domainRequest)).thenReturn(restRequest);

    var actual = subject.processCityJSONRequest(createRequest, REQUEST_ID);

    assertSame(restRequest, actual);
    verify(createCityJSONRequestValidatorMock).accept(createRequest);
  }

  @Test
  void get_city_json_request_by_id_reads_the_request_of_the_community() {
    var restRequest = new CityJSONRequest();
    when(cityJSONRequestServiceMock.getByIdAndCommunityOwnerId(REQUEST_ID, COMMUNITY_ID))
        .thenReturn(domainRequest);
    when(cityJSONRequestMapperMock.toRest(domainRequest)).thenReturn(restRequest);

    var actual = subject.getById(REQUEST_ID);

    assertSame(restRequest, actual);
  }

  @Test
  void requests_of_unknown_api_keys_are_read_without_community() {
    when(communityAuthorizationRepositoryMock.findByApiKey(API_KEY)).thenReturn(Optional.empty());
    when(cityJSONRequestServiceMock.getByIdAndCommunityOwnerId(REQUEST_ID, null))
        .thenReturn(domainRequest);

    var actual = subject.getRequested3DFileById(REQUEST_ID);

    assertSame(restResponse, actual);
  }
}
