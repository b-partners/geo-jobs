package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.BuildingApi;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class GeometryConverterTest {
  GeometryConverter subject = new GeometryConverter(new BuildingApi());

  @Test
  void retrieveRoofPolygonsFrom_ok() {
    List<List<BigDecimal>> polygonCoordinates =
        List.of(
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.652159755838795)),
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.651375009133034)),
            List.of(
                BigDecimal.valueOf(-0.24774244579347737), BigDecimal.valueOf(46.651375009133034)),
            List.of(
                BigDecimal.valueOf(-0.24774244579347737), BigDecimal.valueOf(46.652159755838795)),
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.652159755838795)));
    var actual = subject.retrieveRoofPolygonsFrom(polygonCoordinates);

    log.info(
        "polygon inside or intersects {}",
        actual.stream().map(multiPolygon -> subject.writeGeometryAsString(multiPolygon)).toList());
    assertFalse(actual.isEmpty());
  }
}
