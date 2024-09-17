package app.bpartners.geojobs.service.geo;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class TilesDownloaderIT extends FacadeIT {
  @MockBean BucketComponent bucketComponent;
  @Autowired TilesDownloader httpApiTilesDownloader;
  @Autowired ObjectMapper om;

  private ParcelContent a_parcel_from_lyon(int zoom)
      throws MalformedURLException, JsonProcessingException {
    return ParcelContent.builder()
        .id(randomUUID().toString())
        .geoServerUrl(new URL("https://data.grandlyon.com/fr/geoserv/grandlyon/ows"))
        .geoServerParameter(
            om.readValue(
                """
                {
                    "service": "WMS",
                    "request": "GetMap",
                    "layers": "grandlyon:ortho_2018",
                    "styles": "",
                    "format": "image/jpeg",
                    "version": "1.3.0",
                    "width": 1024,
                    "height": 1024,
                    "srs": "EPSG:3857"
                }""",
                GeoServerParameter.class))
        .feature(
            om.readValue(
                    """
                    { "type": "Feature",
                      "properties": {
                        "code": "69",
                        "nom": "Rhône - 1 sur 1000x100",
                        "id": 30251921,
                        "CLUSTER_ID": 99520,
                        "CLUSTER_SIZE": 386884 },
                      "geometry": {
                        "type": "MultiPolygon",
                        "coordinates": [ [ [
                          [ 4.803193184300449, 45.732156868763205 ],
                          [ 4.802538245115325, 45.732990634128193 ],
                          [ 4.80264872650989, 45.733263461411831 ],
                          [ 4.803125193613379, 45.733382317920366 ],
                          [ 4.803576766482497, 45.73258632485657 ],
                          [ 4.803576472461046, 45.73258224786219 ],
                          [ 4.803193184300449, 45.732156868763205 ] ] ] ] } }""",
                    Feature.class)
                .zoom(zoom)
                .id("feature_1_id"))
        .build();
  }

  private ParcelContent a_parcel_from_cannes(int zoom)
      throws MalformedURLException, JsonProcessingException {
    return ParcelContent.builder()
        .id(randomUUID().toString())
        .geoServerUrl(
            new URL(
                "https://cartolive.ville-cannes.fr/server/services/cache/Ortho_2020_5cm/MapServer/WMSServer"))
        .geoServerParameter(
            om.readValue(
                """
                {
                    "service": "WMS",
                    "request": "GetMap",
                    "layers": "0",
                    "styles": "",
                    "format": "image/jpeg",
                    "version": "1.0.0",
                    "transparent": true,
                    "width": 1024,
                    "height": 1024,
                    "srs": "EPSG:3857"
                }""",
                GeoServerParameter.class))
        .feature(
            om.readValue(
                    """
                                        {
                        "type": "Feature",
                        "properties": {
                            "id": 79589,
                            "CLUSTER_ID": 75394,
                            "CLUSTER_SI": 529,
                            "objectid": 1,
                            "id_2": "5",
                            "nom": "ILE SAINTE MARGUERITE",
                            "st_area_sh": 1707496.1756,
                            "st_length_": 10585.3241542
                        },
                        "geometry": {
                            "type": "MultiPolygon",
                            "coordinates": [
                                [
                                    [
                                        [
                                            7.053824189976548,
                                            43.519987765025689
                                        ],
                                        [
                                            7.053391928619927,
                                            43.520444122594547
                                        ],
                                        [
                                            7.053925229491217,
                                            43.520622151393155
                                        ],
                                        [
                                            7.054267541263264,
                                            43.520400341083466
                                        ],
                                        [
                                            7.054065923731642,
                                            43.519992299731342
                                        ],
                                        [
                                            7.053824189976548,
                                            43.519987765025689
                                        ]
                                    ]
                                ]
                            ]
                        }
                    }""",
                    Feature.class)
                .zoom(zoom)
                .id("feature_1_id"))
        .build();
  }

  private ParcelContent a_parcel_from_cannes_proxy(int zoom)
      throws MalformedURLException, JsonProcessingException {
    return ParcelContent.builder()
        .id(randomUUID().toString())
        .geoServerUrl(new URL("http://35.181.83.111:80/geoserver/cite/wms"))
        .geoServerParameter(
            om.readValue(
                """
                {
                    "service": "WMS",
                    "request": "GetMap",
                    "layers": "cite:cannes_2020",
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
            om.readValue(
                    """
                                        {
                        "type": "Feature",
                        "properties": {
                            "id": 79589,
                            "CLUSTER_ID": 75394,
                            "CLUSTER_SI": 529,
                            "objectid": 1,
                            "id_2": "5",
                            "nom": "ILE SAINTE MARGUERITE",
                            "st_area_sh": 1707496.1756,
                            "st_length_": 10585.3241542
                        },
                        "geometry": {
                            "type": "MultiPolygon",
                            "coordinates": [
                                [
                                    [
                                        [
                                            7.053824189976548,
                                            43.519987765025689
                                        ],
                                        [
                                            7.053391928619927,
                                            43.520444122594547
                                        ],
                                        [
                                            7.053925229491217,
                                            43.520622151393155
                                        ],
                                        [
                                            7.054267541263264,
                                            43.520400341083466
                                        ],
                                        [
                                            7.054065923731642,
                                            43.519992299731342
                                        ],
                                        [
                                            7.053824189976548,
                                            43.519987765025689
                                        ]
                                    ]
                                ]
                            ]
                        }
                    }""",
                    Feature.class)
                .zoom(zoom)
                .id(randomUUID().toString()))
        .build();
  }

  @Test
  @Disabled("TODO: fail")
  public void download_tiles_lyon_ok() throws IOException {
    var zoom = 20;

    var tilesDir = httpApiTilesDownloader.apply(a_parcel_from_lyon(zoom));

    assertEquals(4, new File(tilesDir.getAbsolutePath() + "/" + zoom).listFiles().length);
  }

  // TODO: run locally only
  @Disabled
  @Test
  public void download_tiles_cannes_ok() throws IOException {
    var zoom = 20;

    var tilesDir = httpApiTilesDownloader.apply(a_parcel_from_cannes(zoom));

    assertEquals(4, new File(tilesDir.getAbsolutePath() + "/" + zoom).listFiles().length);
  }
}
