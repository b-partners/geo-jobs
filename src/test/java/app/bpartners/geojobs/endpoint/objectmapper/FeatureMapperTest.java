package app.bpartners.geojobs.endpoint.objectmapper;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
class FeatureMapperTest {
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @SneakyThrows
  @Test
  void feature_with_delimitation_map_domain_to_rest() {
    var domainStringValue =
        """
{"feature": {"id": null, "zoom": null, "geometry": {"geometryType": "Polygon", "actualInstanceStringValue": "{\\"coordinates\\":[[[7.000641313343493,43.55070819734027],[7.000641313343493,43.550288904430346],[7.00155476701002,43.550288904430346],[7.00155476701002,43.55070819734027],[7.000641313343493,43.55070819734027]]],\\"type\\":\\"Polygon\\"}"}, "properties": {}}, "delimitations": [{"id": "a0e46d00-59e1-4aa0-8dc1-ec61aac32e28", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.001148637226904,43.550525052209835],[7.001148055663338,43.55049894787643],[7.001124582264526,43.55049981372897],[7.001126273949966,43.55052407533543],[7.001148637226904,43.550525052209835]]]]}"}, "properties": {}}, {"id": "815c91a3-76b2-4295-a8e1-5310db28384e", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.00114224638695,43.5504333972554],[7.001107716703516,43.550435571822945],[7.00110890714233,43.55045264480464],[7.001113786251782,43.55045156394304],[7.001117429920042,43.5504505286522],[7.001119900802052,43.55045043751022],[7.001123607125052,43.55045030079718],[7.001127376103014,43.55045106266197],[7.001129909640052,43.55045187009774],[7.001133678618198,43.550452631962315],[7.001134976714338,43.55045348496913],[7.001137510251588,43.550454292404744],[7.001138808347822,43.5504551454115],[7.001145298829568,43.550459410445114],[7.00114224638695,43.5504333972554]]]]}"}, "properties": {}}, {"id": "1bbcee6d-cc5d-4bdf-8d08-e9beb9578829", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.000920844154394,43.55036679002985],[7.00100250845384,43.550365579619005],[7.001002347909288,43.55032774820439],[7.001110019011332,43.55032647932477],[7.001114154217408,43.55038578546792],[7.001018962707894,43.550388395809904],[7.001020779651976,43.55041445457214],[7.00100465627479,43.55041414840123],[7.001008074783074,43.55049870587738],[7.000952417232946,43.55049985792279],[7.000950725621664,43.55047559631403],[7.000925954135852,43.550475609114606],[7.000920844154394,43.55036679002985]]]]}"}, "properties": {}}, {"id": "396c2c67-796f-47ce-bab2-5f6066bb9d3e", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.001409385467734,43.55071182764997],[7.001150299949268,43.55070877227807],[7.001143004582222,43.55063967290642],[7.00116400714788,43.55063889819283],[7.001158197839128,43.55057334756624],[7.001233497239312,43.550569669104384],[7.001235484564402,43.55058040646799],[7.00124907444506,43.55057990517163],[7.00125133009923,43.55061225397956],[7.001234033877726,43.55061289199318],[7.001235036387186,43.550627269241545],[7.001241150953786,43.55062614280176],[7.001269834513988,43.550646706078226],[7.001271383246328,43.550651153395584],[7.001406360059308,43.55065067877436],[7.001409385467734,43.55071182764997]]]]}"}, "properties": {}}, {"id": "9681f580-ce9f-44c6-9c98-f7369b0e0d11", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.001150299949268,43.55070877227807],[7.001065976308774,43.550707378206646],[7.001067203850472,43.550636162680654],[7.001101921620386,43.55063668386003],[7.001102181942082,43.55058712535249],[7.001140480703814,43.55058571265089],[7.001143004582222,43.55063967290642],[7.001150299949268,43.55070877227807]]]]}"}, "properties": {}}, {"id": "12a4faee-3bc5-4d11-8c2b-b5f771eb50ce", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.000871710085434,43.550532564100074],[7.000871244033478,43.55056140973836],[7.000941852271356,43.55056150806415],[7.000941090593504,43.55060387794497],[7.000870544954664,43.55060467819688],[7.00087022178183,43.550653338126246],[7.000778548205616,43.55065311581862],[7.000778029458674,43.55062791005915],[7.00076067057776,43.55062764941906],[7.000761495038432,43.55058617811709],[7.000772676685796,43.55058666659249],[7.000773796837262,43.550531671051],[7.000871710085434,43.550532564100074]]]]}"}, "properties": {}}, {"id": "1012727e-f1c8-4ad3-97fa-bc5e7deab0c3", "zoom": 20, "geometry": {"geometryType": "MultiPolygon", "actualInstanceStringValue": "{\\"type\\":\\"MultiPolygon\\",\\"coordinates\\":[[[[7.000751624024868,43.55040906707388],[7.00084193647015,43.55040753781581],[7.000843770904132,43.550451613711914],[7.00075345838959,43.55045314297158],[7.000751624024868,43.55040906707388]]]]}"}, "properties": {}}]}""";

    var featureWithDelimitation =
        objectMapper.readValue(domainStringValue, FeatureWithDelimitation.class);

    log.info(
        "feature delimitations {}",
        featureWithDelimitation.delimitations().stream()
            .map(
                feature -> {
                  var restFeature = toRestFeature(feature);
                  try {
                    return objectMapper.writeValueAsString(restFeature);
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList());
    assertNotNull(featureWithDelimitation);
  }
}
