package app.bpartners.geojobs.repository.model.geo;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTIPOLYGON;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.ParcelContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParcelIT extends FacadeIT {

  @Autowired ObjectMapper om;

  @Test
  void serialize_then_deserialize() throws JsonProcessingException, MalformedURLException {
    var parcel =
        ParcelContent.builder()
            .geoServerUrl(new URL("https://nowhere.com"))
            .feature(new Feature().geometry(new MultiPolygon().type(MULTIPOLYGON)))
            .geoServerParameter(new GeoServerParameter().height(1024))
            .build();

    var serialized = om.writeValueAsString(parcel);
    var deserialized = om.readValue(serialized, ParcelContent.class);

    assertEquals(parcel.toString(), deserialized.toString());
  }
}
