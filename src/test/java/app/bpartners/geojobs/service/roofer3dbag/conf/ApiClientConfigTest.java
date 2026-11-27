package app.bpartners.geojobs.service.roofer3dbag.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;

class ApiClientConfigTest {
  private static final String URL = "http://roofer.test/ping";

  @Test
  void roofer_rest_template_sends_the_api_key_header() {
    var restTemplate =
        new ApiClientConfig(new RooferApiProperties("http://roofer.test", "key"))
            .rooferRestTemplate();
    var server = MockRestServiceServer.bindTo(restTemplate).build();
    server
        .expect(method(GET))
        .andExpect(header("x-api-key", "key"))
        .andRespond(withSuccess("pong", org.springframework.http.MediaType.TEXT_PLAIN));

    var actual = restTemplate.getForObject(URL, String.class);

    assertEquals("pong", actual);
    server.verify();
  }

  @Test
  void roofer_rest_template_sends_no_api_key_header_when_the_key_is_blank() {
    var restTemplate =
        new ApiClientConfig(new RooferApiProperties("http://roofer.test", " "))
            .rooferRestTemplate();
    var server = MockRestServiceServer.bindTo(restTemplate).build();
    server
        .expect(method(GET))
        .andExpect(headerDoesNotExist("x-api-key"))
        .andRespond(withSuccess("pong", org.springframework.http.MediaType.TEXT_PLAIN));

    var actual = restTemplate.getForObject(URL, String.class);

    assertEquals("pong", actual);
    server.verify();
  }

  @Test
  void roofer_rest_template_is_built_even_without_api_key() {
    var restTemplate =
        new ApiClientConfig(new RooferApiProperties("http://roofer.test", null))
            .rooferRestTemplate();

    assertEquals(1, restTemplate.getInterceptors().size());
  }
}
