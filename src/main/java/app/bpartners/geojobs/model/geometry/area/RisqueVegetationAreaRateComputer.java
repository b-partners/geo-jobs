package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;

@Getter
@RequiredArgsConstructor
public class RisqueVegetationAreaRateComputer {
  private static final double SAFE_VEGETATION_DISTANCE = 30; // in meter
  private final Geometry roofGeometry;
  private final DetectedTile tile;
  private final Collection<PolygonObjectType> polygonObjectTypes;
  private final GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();

  public RisqueVegetationAreaRateComputer(Geometry roofGeometry, DetectedTile tile) {
    this.roofGeometry = roofGeometry;
    this.tile = tile;
    this.polygonObjectTypes = null;
  }

  public RisqueVegetationAreaRateComputer(
      Geometry roofGeometry, Collection<PolygonObjectType> polygonObjectTypes) {
    this.roofGeometry = roofGeometry;
    this.polygonObjectTypes = polygonObjectTypes;
    this.tile = null;
  }

  public IndiceVegetation getIndiceVegetation() {
    VegetationDistanceClass vegetationDistanceClass =
        VegetationDistanceClass.fromDistance(minDistanceFromBati());
    VegetationMassClass vegetationMassClass =
        VegetationMassClass.fromArea(vegetationAreaAt30Meters());

    return switch (vegetationDistanceClass) {
      case COLLEE_AU_BATI ->
          switch (vegetationMassClass) {
            case TRES_FAIBLE -> IndiceVegetation.MODEREE;
            case FAIBLE, MOYEN, FORTE -> IndiceVegetation.ELEVEE;
          };
      case PROCHE ->
          switch (vegetationMassClass) {
            case TRES_FAIBLE -> IndiceVegetation.FAIBLE;
            case FAIBLE, MOYEN -> IndiceVegetation.MODEREE;
            case FORTE -> IndiceVegetation.ELEVEE;
          };
      case ENVIRONNEMENT_PROCHE ->
          switch (vegetationMassClass) {
            case TRES_FAIBLE -> IndiceVegetation.NULLE;
            case FAIBLE -> IndiceVegetation.FAIBLE;
            case MOYEN -> IndiceVegetation.MODEREE;
            case FORTE -> IndiceVegetation.ELEVEE;
          };
      case LOINTAINE ->
          switch (vegetationMassClass) {
            case TRES_FAIBLE, FAIBLE -> IndiceVegetation.NULLE;
            case MOYEN -> IndiceVegetation.FAIBLE;
            case FORTE -> IndiceVegetation.MODEREE;
          };
    };
  }

  private double minDistanceFromBati() {
    Polygon meteredBatiPolygon = (Polygon) toMeterUnit(roofGeometry);
    List<Polygon> meteredVegetationPolygons = getMeteredVegetationPolygons();

    Optional<Double> minDistanceFromBatiArea =
        meteredVegetationPolygons.stream()
            .map(meteredPolygon -> meteredPolygon.distance(meteredBatiPolygon))
            .min(Double::compare);
    if (minDistanceFromBatiArea.isEmpty()) {
      throw new IllegalStateException("No vegetation areas found");
    }

    return minDistanceFromBatiArea.get();
  }

  private double vegetationAreaAt30Meters() {
    List<Polygon> meteredVegetationPolygons = getMeteredVegetationPolygons();
    Polygon unifiedVegetationPolygon = (Polygon) UnaryUnionOp.union(meteredVegetationPolygons);
    Polygon batiPolygon30mBuffered = (Polygon) buffer30m(roofGeometry);

    Polygon vegetationAreaAt30m =
        (Polygon) unifiedVegetationPolygon.difference(batiPolygon30mBuffered);

    return vegetationAreaAt30m.getArea();
  }

  private List<Polygon> getMeteredVegetationPolygons() {
    return getVegetationPolygonObjectTypes().stream()
        .map(po -> (Polygon) toMeterUnit(po.polygon()))
        .toList();
  }

  private List<PolygonObjectType> getVegetationPolygonObjectTypes() {
    if (polygonObjectTypes == null) {
      throw new IllegalArgumentException("No polygon object types found");
    }
    List<DetectableType> vegetationTypes =
        List.of(DetectableType.ESPACE_VERT, DetectableType.ARBRE, DetectableType.ESPACE_ARBORE);
    return polygonObjectTypes.stream()
        .filter(p -> vegetationTypes.contains(p.objectType()))
        .toList();
  }

  private Geometry toMeterUnit(Geometry geometry) {
    return geometrySquareMeterArea.project(geometry, WGS84, LAMBERT_93);
  }

  private Geometry buffer30m(Geometry geometry) {
    Geometry projected = toMeterUnit(geometry);
    return projected.buffer(SAFE_VEGETATION_DISTANCE);
  }
}
