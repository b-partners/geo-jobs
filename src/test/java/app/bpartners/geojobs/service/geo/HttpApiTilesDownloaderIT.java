package app.bpartners.geojobs.service.geo;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.tiling.downloader.HttpApiTilesDownloader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class HttpApiTilesDownloaderIT extends FacadeIT {
  @MockBean BucketComponent bucketComponent;
  @Autowired
  HttpApiTilesDownloader httpApiTilesDownloader;
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

  @Test
  public void download_tiles_ok() throws IOException {
    var zoom = 20;

    var tilesDir = httpApiTilesDownloader.apply(a_parcel_from_lyon(zoom));

    assertEquals(
        4,
        Objects.requireNonNull(new File(tilesDir.getAbsolutePath() + "/" + zoom).listFiles())
            .length);
  }
}
