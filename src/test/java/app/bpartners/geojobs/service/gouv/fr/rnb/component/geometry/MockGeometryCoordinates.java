package app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry;

import java.math.BigDecimal;
import java.util.List;

public class MockGeometryCoordinates {

  public static PointCoordinates getPointCoordinates() {
    return new PointCoordinates(
        List.of(BigDecimal.valueOf(2.330100574198268), BigDecimal.valueOf(48.87783810604481)));
  }

  public static MultiPolygonCoordinates getMultiPolygonCoordinates() {
    return new MultiPolygonCoordinates(
        List.of(
            List.of(
                List.of(
                    List.of(new BigDecimal("2.3299732"), new BigDecimal("48.877899399999976")),
                    List.of(new BigDecimal("2.3300557"), new BigDecimal("48.87790939999997")),
                    List.of(new BigDecimal("2.3300529"), new BigDecimal("48.87791539999997")),
                    List.of(
                        new BigDecimal("2.330148722559339"), new BigDecimal("48.87793466432709")),
                    List.of(
                        new BigDecimal("2.330149851944901"), new BigDecimal("48.87793282932168")),
                    List.of(
                        new BigDecimal("2.330236394657511"), new BigDecimal("48.877776829077824")),
                    List.of(
                        new BigDecimal("2.330043360045236"), new BigDecimal("48.877731681471396")),
                    List.of(
                        new BigDecimal("2.329973060965061"), new BigDecimal("48.8778993830118")),
                    List.of(new BigDecimal("2.3299732"), new BigDecimal("48.877899399999976"))))));
  }
}
