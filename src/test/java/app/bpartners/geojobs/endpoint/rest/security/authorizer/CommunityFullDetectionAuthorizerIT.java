package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static org.mockito.Mockito.mockStatic;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;

class CommunityFullDetectionAuthorizerIT extends FacadeIT {
  private final MockedStatic<AuthProvider> authProvider = mockStatic(AuthProvider.class);
  @Autowired CommunityFullDetectionAuthorizer communityFullDetectionAuthorizer;

  private static Feature feature1555SurfaceInside2000() {
    Feature feature = new Feature();
    var sideLength = Math.sqrt(1_555);
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.5)),
                List.of(BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.5 + sideLength)),
                List.of(BigDecimal.valueOf(2.5 + sideLength), BigDecimal.valueOf(2.5 + sideLength)),
                List.of(BigDecimal.valueOf(2.5 + sideLength), BigDecimal.valueOf(2.5)),
                List.of(BigDecimal.valueOf(2.5), BigDecimal.valueOf(2.5))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(multiPolygon);
    return feature;
  }

  private static Feature feature50SurfaceOutside2000() {
    Feature feature = new Feature();
    var sideLength = Math.sqrt(50);
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5)),
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5 + sideLength)),
                List.of(BigDecimal.valueOf(40 + sideLength), BigDecimal.valueOf(-5 + sideLength)),
                List.of(BigDecimal.valueOf(40 + sideLength), BigDecimal.valueOf(-5)),
                List.of(BigDecimal.valueOf(40), BigDecimal.valueOf(-5))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(multiPolygon);
    return feature;
  }

  private static Feature feature2000Surface() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(44.72)),
                List.of(BigDecimal.valueOf(44.72), BigDecimal.valueOf(44.72)),
                List.of(BigDecimal.valueOf(44.72), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(List.of(coordinates));
    feature.setGeometry(multiPolygon);
    return feature;
  }

  @AfterEach
  void cleanMock() {
    authProvider.close();
  }
}
