package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@AllArgsConstructor
@Service
public class GeoJsonContinuerService {

    private final LatLonLinesContinuer latLonLinesContinuer = new LatLonLinesContinuer(
            new RoutesContinuationConf(
                    new AlphaConf(5,9),
                    new UnionConf(2),
                    new ContinuationConf(2,3,5),
                    new PrettyConf(5)
            ),
            new TilingConf(1,5),
            6
    );

    public Geojson continueGeojson(Geojson geoJsonToContinue) {
        Set<LatLonPolygon> features = latLonLinesContinuer.apply(geoJsonToContinue.polygons());
        return new Geojson(features);
    }
}

