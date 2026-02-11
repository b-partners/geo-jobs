package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class AreaRateComputerFacadeTest extends AreaRateComputerTest {
  @Test
  void get_usure_area_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100
    var usureLeger = createSquare(1); // Area = 1
    var usureImportante = createSquare(2); // Area = 4

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(usureLeger, USURE_LEGER),
            new PolygonObjectType(usureImportante, USURE_IMPORTANTE));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    // (1 * 1 + 2 * 4) / 100 * 100 = 9.0
    assertEquals(9.0, subject.getUsureAreaRate());
  }

  @Test
  void get_moisissure_area_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100
    var mNoircie = createSquare(1); // Area = 1
    var mClair = createSquare(2); // Area = 4
    var mCouleur = createSquare(3); // Area = 9

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(mNoircie, MOISISSURE_NOIRCIE),
            new PolygonObjectType(mClair, MOISISSURE_CLAIR),
            new PolygonObjectType(mCouleur, MOISISSURE_COULEUR));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    // (1 + 4 + 9) / 100 * 100 = 14.0
    assertEquals(14.0, subject.getMoisissureAreaRate());
  }

  @Test
  void get_humidity_area_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100
    var hClair = createSquare(1); // Area = 1
    var hIntense = createSquare(2); // Area = 4

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(hClair, HUMIDITE_CLAIR),
            new PolygonObjectType(hIntense, HUMIDITE_INTENSE));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    // (1 * 1 + 2 * 4) / 100 * 100 = 9.0
    assertEquals(9.0, subject.getHumidityAreaRate());
  }

  @Test
  void get_global_rate_returns_expected_value() {
    var roof = createSquare(10); // Area = 100

    // Usure: (1*1 + 0) / 100 * 100 = 1.0. Global weight 0.4 -> 0.4
    // Moisissure: (1*1 + 0 + 0) / 100 * 100 = 1.0. Global weight 0.8 -> 0.8
    // Humidite: (1*1 + 0) / 100 * 100 = 1.0. Global weight 1.0 -> 1.0
    // Total Global = 0.4 + 0.8 + 1.0 = 2.2

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(createSquare(1), USURE_LEGER),
            new PolygonObjectType(createSquare(1), MOISISSURE_NOIRCIE),
            new PolygonObjectType(createSquare(1), HUMIDITE_CLAIR));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    assertEquals(2.2, subject.getGlobalRate());
  }

  @Test
  void test_with_example_data() {
    // Surface totale du toit : 100 m²
    var roof = createSquare(10);

    // Usure légère : 10 m², Usure importante : 5 m²
    // Taux_usure : (10 × 1 + 5 × 2) / 100 = 20 %
    var usureLegere = createSquare(Math.sqrt(10));
    var usureImportante = createSquare(Math.sqrt(5));

    // Moisissure couleur : 2 m², Moisissure clair : 3 m², Moisissure noircie : 5 m²
    // Taux_moisissure : (2 + 3 + 5) / 100 = 10 %
    var mCouleur = createSquare(Math.sqrt(2));
    var mClair = createSquare(Math.sqrt(3));
    var mNoircie = createSquare(Math.sqrt(5));

    // Humidité claire : 4 m², Humidité intense : 6 m²
    // Taux_humidite : (4 × 1 + 6 × 2) / 100 = 16 %
    var hClaire = createSquare(Math.sqrt(4));
    var hIntense = createSquare(Math.sqrt(6));

    var polygonObjectTypes =
        List.of(
            new PolygonObjectType(usureLegere, USURE_LEGER),
            new PolygonObjectType(usureImportante, USURE_IMPORTANTE),
            new PolygonObjectType(mCouleur, MOISISSURE_COULEUR),
            new PolygonObjectType(mClair, MOISISSURE_CLAIR),
            new PolygonObjectType(mNoircie, MOISISSURE_NOIRCIE),
            new PolygonObjectType(hClaire, HUMIDITE_CLAIR),
            new PolygonObjectType(hIntense, HUMIDITE_INTENSE));

    var subject = new AreaRateComputerFacade(roof, polygonObjectTypes);

    assertEquals(20.0, subject.getUsureAreaRate());
    assertEquals(10.0, subject.getMoisissureAreaRate());
    assertEquals(16.0, subject.getHumidityAreaRate());

    // Penalty = 0.4 * 20 + 0.8 * 10 + 1.0 * 16 = 8 + 8 + 16 = 32
    assertEquals(32.0, subject.getGlobalRate());
  }

  @Test
  void get_rate_returns_correct_enum_values() {
    var roof = createSquare(10);

    // Rate A: global < 4
    assertEquals(Rate.A, new AreaRateComputerFacade(roof, List.of()).getRate());

    // Rate B: 4 <= global < 11
    // Humidite 5% -> global 5.0
    assertEquals(
        Rate.B,
        new AreaRateComputerFacade(
                roof,
                List.of(
                    new PolygonObjectType(
                        geometryFactory.createPolygon(
                            new Coordinate[] {
                              new Coordinate(0, 0),
                              new Coordinate(2.236, 0),
                              new Coordinate(2.236, 2.236),
                              new Coordinate(0, 2.236),
                              new Coordinate(0, 0)
                            }),
                        HUMIDITE_CLAIR) // Area ~ 5
                    ))
            .getRate());

    // Rate C: 11 <= global < 21
    // Humidite 15% -> global 15.0
    assertEquals(
        Rate.C,
        new AreaRateComputerFacade(
                roof,
                List.of(
                    new PolygonObjectType(
                        geometryFactory.createPolygon(
                            new Coordinate[] {
                              new Coordinate(0, 0),
                              new Coordinate(3.873, 0),
                              new Coordinate(3.873, 3.873),
                              new Coordinate(0, 3.873),
                              new Coordinate(0, 0)
                            }),
                        HUMIDITE_CLAIR) // Area ~ 15
                    ))
            .getRate());

    // Rate D: 21 <= global < 41
    // Humidite 30% -> global 30.0
    assertEquals(
        Rate.D,
        new AreaRateComputerFacade(
                roof,
                List.of(
                    new PolygonObjectType(
                        geometryFactory.createPolygon(
                            new Coordinate[] {
                              new Coordinate(0, 0),
                              new Coordinate(5.477, 0),
                              new Coordinate(5.477, 5.477),
                              new Coordinate(0, 5.477),
                              new Coordinate(0, 0)
                            }),
                        HUMIDITE_CLAIR) // Area ~ 30
                    ))
            .getRate());

    // Rate E: global >= 41
    // Humidite 50% -> global 50.0
    assertEquals(
        Rate.E,
        new AreaRateComputerFacade(
                roof,
                List.of(
                    new PolygonObjectType(
                        geometryFactory.createPolygon(
                            new Coordinate[] {
                              new Coordinate(0, 0),
                              new Coordinate(7.071, 0),
                              new Coordinate(7.071, 7.071),
                              new Coordinate(0, 7.071),
                              new Coordinate(0, 0)
                            }),
                        HUMIDITE_CLAIR) // Area ~ 50
                    ))
            .getRate());
  }

  @Test
  void format_rounds_to_two_decimal_places() {
    assertEquals(1.23, AreaRateComputerFacade.format(1.2345));
    assertEquals(1.24, AreaRateComputerFacade.format(1.2355));
    assertEquals(1.0, AreaRateComputerFacade.format(1.001));
  }
}
