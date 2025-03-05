package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.toLatLon;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.SneakyThrows;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;

public class GeoReferencer {
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_1_SFS =
      readFranceDepartmentsFeatureCollectionAsList(1);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_2_SFS =
      readFranceDepartmentsFeatureCollectionAsList(2);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_3_SFS =
      readFranceDepartmentsFeatureCollectionAsList(3);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_4_SFS =
      readFranceDepartmentsFeatureCollectionAsList(4);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_5_SFS =
      readFranceDepartmentsFeatureCollectionAsList(5);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_6_SFS =
      readFranceDepartmentsFeatureCollectionAsList(6);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_7_SFS =
      readFranceDepartmentsFeatureCollectionAsList(7);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_8_SFS =
      readFranceDepartmentsFeatureCollectionAsList(8);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_9_SFS =
      readFranceDepartmentsFeatureCollectionAsList(9);
  private static final List<SimpleFeature> FRANCE_DEPARTMENTS_10_SFS =
      readFranceDepartmentsFeatureCollectionAsList(10);

  private GeoReferencer() {}

  public static List<BigDecimal> toGeographicalCoordinates(
      int xTile, int yTile, double x, double y, int zoom, int imageWidth) {
    var originTile = new IntXY(xTile, yTile);
    var tilingConf = new TilingConf(zoom, imageWidth);
    var pixel = new IntXY((int) x, (int) y);
    var coordinate = toLatLon(originTile, tilingConf, pixel);
    return List.of(BigDecimal.valueOf(coordinate.y), BigDecimal.valueOf(coordinate.x));
  }

  @SneakyThrows
  private static List<SimpleFeature> readFranceDepartmentsFeatureCollectionAsList(int number) {
    return getSimpleFeatures("departments_%s.json".formatted(number));
  }

  public static List<SimpleFeature> getFranceDepartmentsSimpleFeaturesMatchingPredicate(
      Predicate<SimpleFeature> predicate) {
    var result = new ArrayList<SimpleFeature>();
    var allLists =
        List.of(
            FRANCE_DEPARTMENTS_1_SFS,
            FRANCE_DEPARTMENTS_2_SFS,
            FRANCE_DEPARTMENTS_3_SFS,
            FRANCE_DEPARTMENTS_4_SFS,
            FRANCE_DEPARTMENTS_5_SFS,
            FRANCE_DEPARTMENTS_6_SFS,
            FRANCE_DEPARTMENTS_7_SFS,
            FRANCE_DEPARTMENTS_8_SFS,
            FRANCE_DEPARTMENTS_9_SFS,
            FRANCE_DEPARTMENTS_10_SFS);
    var matcherFunction = matchPredicate(predicate);
    allLists.forEach(list -> result.addAll(matcherFunction.apply(list)));
    return result;
  }

  private static Function<List<SimpleFeature>, List<SimpleFeature>> matchPredicate(
      Predicate<SimpleFeature> predicate) {
    return list -> list.stream().filter(predicate).toList();
  }

  @NotNull
  private static List<SimpleFeature> getSimpleFeatures(String geojsonFileName) throws IOException {
    var classPathResource = new ClassPathResource("files/france-geojson/" + geojsonFileName);
    InputStream inputStream = classPathResource.getInputStream();
    FeatureJSON featureJSON = new FeatureJSON();
    var simpleFeatureCollection =
        (SimpleFeatureCollection) featureJSON.readFeatureCollection(inputStream);
    List<SimpleFeature> res = new ArrayList<>();
    try (var features = simpleFeatureCollection.features()) {
      while (features.hasNext()) {
        res.add(features.next());
      }
    }
    return res;
  }
}
