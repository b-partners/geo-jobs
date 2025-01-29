package app.bpartners.geojobs.service.geo;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class TilesDownloaderIT extends FacadeIT {
  @MockBean BucketComponent bucketComponent;
  @Autowired TilesDownloader httpApiTilesDownloader;
  @Autowired ObjectMapper om;

  private ParcelContent a_parcel_from_cannes(int zoom)
      throws MalformedURLException, JsonProcessingException {
    List<List<List<List<BigDecimal>>>> coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(
                        new BigDecimal("7.053824189976548"), new BigDecimal("43.519987765025689")),
                    List.of(
                        new BigDecimal("7.053391928619927"), new BigDecimal("43.520444122594547")),
                    List.of(
                        new BigDecimal("7.053925229491217"), new BigDecimal("43.520622151393155")),
                    List.of(
                        new BigDecimal("7.054267541263264"), new BigDecimal("43.520400341083466")),
                    List.of(
                        new BigDecimal("7.054065923731642"), new BigDecimal("43.519992299731342")),
                    List.of(
                        new BigDecimal("7.053824189976548"),
                        new BigDecimal("43.519987765025689")))));

    return ParcelContent.builder()
        .id(randomUUID().toString())
        .geoServerUrl(new URL("http://35.181.83.111:80/geoserver/cite/wms"))
        .geoServerParameter(
            om.readValue(
                """
                {
                    "service": "WMS",
                    "request": "GetMap",
                    "layers": "cite:ALPES-MARITIMES_CANNES_2020_5cm",
                    "styles": "",
                    "format": "image/jpeg",
                    "version": "1.1.0",
                    "transparent": true,
                    "width": 1024,
                    "height": 1024,
                    "srs": "EPSG:4326"
                }""",
                GeoServerParameter.class))
        .feature(
            Feature.builder()
                .id(randomUUID().toString())
                .geometry(
                    Feature.FeatureGeometry.builder()
                        .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                        .actualInstanceStringValue(
                            om.writeValueAsString(
                                new MultiPolygon()
                                    .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
                                    .coordinates(coordinates)))
                        .build())
                .zoom(zoom)
                .build())
        .build();
  }

  @Test
  void download_tiles_cannes_ok() throws IOException {
    var zoom = 20;

    var tilesDir = httpApiTilesDownloader.apply(a_parcel_from_cannes(zoom));

    assertEquals(4, new File(tilesDir.getAbsolutePath() + "/" + zoom).listFiles().length);
  }
}
