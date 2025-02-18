package app.bpartners.geojobs.model.geometry.route;

import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

@Slf4j
@Accessors(fluent = true)
@Getter
public class RoutesContinuation {
  private final Set<Polygon> routes;

  private final AlphaConf alphaConf;
  private final Set<AbstractRoute> abstractions;
  private final ContinuationConf continuationConf;
  private final UnionConf unionConf;
  private final Set<OrientedQuadrilateral> abstractContinuations;

  private final Set<Polygon> continuations;
  private final PrettyConf prettyConf;
  private final Set<Polygon> continued;

  public RoutesContinuation(Set<Polygon> routes, RoutesContinuationConf conf) {
    this.routes = routes;
    this.alphaConf = conf.alphaConf();
    this.unionConf = conf.unionConf();
    this.continuationConf = conf.continuationConf();
    this.prettyConf = conf.prettyConf();

    var abstractRoutesByPolygon = alpha(routes, alphaConf);
    this.abstractions = new HashSet<>(abstractRoutesByPolygon.values());
    this.abstractContinuations = abstractContinuations(abstractRoutesByPolygon, continuationConf);
    this.continuations = continuations(abstractContinuations);

    var toUnify = new HashSet<>(routes);
    toUnify.addAll(continuations);
    var unified = new UnifiedRoute(toUnify, unionConf).unified();
    this.continued = pretty(unified);
  }

  private Set<Polygon> pretty(Set<Polygon> unified) {
    return unified.stream()
        .map(
            p -> {
              var prettyP =
                  (Polygon) DouglasPeuckerSimplifier.simplify(p, prettyConf().dpbThreshold());
              prettyP.setUserData(p.getUserData());
              return prettyP;
            })
        .collect(toSet());
  }

  private Set<Polygon> continuations(Set<OrientedQuadrilateral> abstractContinuations) {
    return abstractContinuations.stream().map(oq -> oq.quadrilateral().polygon()).collect(toSet());
  }

  private static Map<Polygon, AbstractRoute> alpha(Set<Polygon> routes, AlphaConf alphaConf) {
    var abstractedNb = 0;
    var routesSize = routes.size();

    Map<Polygon, AbstractRoute> res = new HashMap<>();
    for (Polygon r : routes) {
      res.put(r, new AbstractRoute(r, alphaConf));
      abstractedNb++;
      if (abstractedNb % 1_000 == 0 || abstractedNb == routesSize) {
        log.info(String.format("Abstracted=%d/%d", abstractedNb, routesSize));
      }
    }
    return res;
  }

  private Set<OrientedQuadrilateral> abstractContinuations(
      Map<Polygon, AbstractRoute> abstractRoutesByPolygon, ContinuationConf continuationConf) {
    Set<OrientedQuadrilateral> res = new HashSet<>();

    var routesAsList = abstractRoutesByPolygon.keySet().stream().toList();
    for (int i = 0; i < routesAsList.size(); i++) {
      for (int j = i + 1; j < routesAsList.size(); j++) {
        var ri = routesAsList.get(i);
        var rj = routesAsList.get(j);
        if (ri.distance(rj) > continuationConf.distanceThreshold()
            || new UnifiedRoute(Set.of(ri, rj), unionConf).unified().size() == 1) {
          continue;
        }

        var continuation =
            new AbstractRouteContinuation(
                abstractRoutesByPolygon.get(ri), abstractRoutesByPolygon.get(rj), continuationConf);
        res.addAll(continuation.continuations());
      }
    }

    return res;
  }
}
