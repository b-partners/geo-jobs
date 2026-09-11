package app.bpartners.geojobs.service.cityjson.texture;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_3812;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class CityJsonTextureDomainServiceTest {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final GeometrySquareMeterArea projector = new GeometrySquareMeterArea();
  private static final CityJsonTextureDomainService subject =
      new CityJsonTextureDomainService(objectMapper, projector);

  @Test
  void resolvesLambert93ByDefault() {
    var cityJson = cityJsonWithReferenceSystem(null);

    var actual = subject.toCityJsonWithVertices(cityJson);

    assertEquals(LAMBERT_93, actual.crs());
  }

  @Test
  void resolvesWgs84() {
    var cityJson = cityJsonWithReferenceSystem("urn:ogc:def:crs:EPSG::4326");

    var actual = subject.toCityJsonWithVertices(cityJson);

    assertEquals(WGS84, actual.crs());
  }

  @Test
  void resolvesLambert93() {
    var cityJson = cityJsonWithReferenceSystem("urn:ogc:def:crs:EPSG::2154");

    var actual = subject.toCityJsonWithVertices(cityJson);

    assertEquals(LAMBERT_93, actual.crs());
  }

  @Test
  void resolvesSwissCrs() {
    var cityJson = cityJsonWithReferenceSystem("urn:ogc:def:crs:EPSG::2056");

    var actual = subject.toCityJsonWithVertices(cityJson);

    assertEquals(EPSG_2056, actual.crs());
  }

  @Test
  void resolvesBelgianLambert2008Crs() {
    var cityJson = cityJsonWithReferenceSystem("urn:ogc:def:crs:EPSG::3812");

    var actual = subject.toCityJsonWithVertices(cityJson);

    assertEquals(EPSG_3812, actual.crs());
  }

  private static ObjectNode cityJsonWithReferenceSystem(String referenceSystem) {
    var json = objectMapper.createObjectNode();
    json.set("vertices", objectMapper.createArrayNode());

    if (referenceSystem != null) {
      var metadata = objectMapper.createObjectNode();
      metadata.put("referenceSystem", referenceSystem);
      json.set("metadata", metadata);
    }

    return json;
  }
}
