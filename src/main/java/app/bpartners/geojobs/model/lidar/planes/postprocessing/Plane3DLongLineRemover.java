package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.*;
import java.util.function.Function;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryCombiner;

@RequiredArgsConstructor
public class Plane3DLongLineRemover implements Function<Polygon, Polygon> {
  private final Plane3DLongLineRemoverConf conf;

  public List<Plane3D> apply(Collection<Plane3D> planes) {
    return planes.stream()
        .map(
            plane -> {
              var delimitation = plane.getDelimitation();
              var newDelimitation = this.apply(delimitation);

              if (delimitation == newDelimitation) {
                return plane;
              }

              newDelimitation = project(plane, newDelimitation);
              return plane.toBuilder()
                  .area(null)
                  .convexDelimitation(null)
                  .delimitation(newDelimitation)
                  .build();
            })
        .toList();
  }

  @Override
  public Polygon apply(Polygon polygon) {
    if (polygon.getArea() <= conf.minAreaToCheck()) {
      return polygon;
    }

    var grid = createGrid(polygon);
    var gridClassification = classify(grid);

    return getGeometry(gridClassification.toKeep(), grid);
  }

  private GridClassification getExtendedGridClassification(GridClassification base) {
    var extended = base.toBuilder().build();
    for (var toDelete : extended.toDelete()) {
      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          if (dx == 0 && dy == 0) continue;
          var neighbor = new CellIndex(toDelete.x() + dx, toDelete.y() + dy);
          if (extended.invalidGrid().contains(neighbor)) {
            extended.toKeep().remove(neighbor);
            extended.toDelete().add(neighbor);
          }
        }
      }
    }
    return extended;
  }

  private Grid createGrid(Polygon polygon) {
    var grid = new Grid(new HashMap<>());

    var env = polygon.getEnvelopeInternal();
    int minX = (int) Math.floor(env.getMinX() / conf.gridSize());
    int maxX = (int) Math.ceil(env.getMaxX() / conf.gridSize());
    int minY = (int) Math.floor(env.getMinY() / conf.gridSize());
    int maxY = (int) Math.ceil(env.getMaxY() / conf.gridSize());

    for (int gx = minX; gx <= maxX; gx++) {
      for (int gy = minY; gy <= maxY; gy++) {
        var cell = createCellPolygon(gx, gy, conf.gridSize());

        if (!polygon.intersects(cell)) continue;

        var intersection = polygon.intersection(cell);
        if (!intersection.isEmpty()) {
          grid.put(new CellIndex(gx, gy), intersection);
        }
      }
    }

    return grid;
  }

  private List<Geometry> getValidNeighbors(CellIndex cell, Grid grid) {
    List<Geometry> neighbors = new ArrayList<>();

    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        if (dx == 0 && dy == 0) continue;

        var idx = new CellIndex(cell.x() + dx, cell.y() + dy);
        var neighbor = grid.get(idx);

        if (neighbor != null && neighbor.getArea() >= conf.cellMin2DArea()) {
          neighbors.add(neighbor);
        }
      }
    }

    return neighbors;
  }

  private GridClassification classify(Grid grid) {
    Set<CellIndex> toKeep = new HashSet<>();
    Set<CellIndex> toDelete = new HashSet<>();
    Set<CellIndex> invalidGrid = new HashSet<>();

    for (var entry : grid.data().entrySet()) {
      var idx = entry.getKey();
      var cellPolygon = entry.getValue();

      if (cellPolygon.getArea() >= conf.cellMin2DArea()) {
        toKeep.add(idx);
        continue;
      }

      invalidGrid.add(idx);
      var neighbors = getValidNeighbors(idx, grid);
      if (neighbors.size() >= conf.cellMinNeighborsCount()) {
        toKeep.add(idx);
      } else {
        toDelete.add(idx);
      }
    }

    var baseGridClassification = new GridClassification(toKeep, toDelete, invalidGrid);
    return getExtendedGridClassification(baseGridClassification);
  }

  private static Polygon getGeometry(Collection<CellIndex> idx, Grid grid) {
    var geometries = idx.stream().map(grid::get).toList();
    var merged = GeometryCombiner.combine(geometries);
    var cleaned = merged.buffer(0);
    return getLargestPolygon(cleaned);
  }

  private static Polygon getLargestPolygon(Geometry geometry) {
    if (geometry instanceof Polygon polygon) return polygon;
    if (geometry instanceof MultiPolygon multi) {
      double maxArea = -1;
      Polygon best = null;

      for (int i = 0; i < multi.getNumGeometries(); i++) {
        var polygon = (Polygon) multi.getGeometryN(i);
        double area = polygon.getArea();

        if (area > maxArea) {
          maxArea = area;
          best = polygon;
        }
      }

      return best;
    }
    throw new RuntimeException("Geometry Type Not Supported");
  }

  private static Polygon createCellPolygon(int gx, int gy, double size) {
    var coordinates =
        new Coordinate[] {
          new Coordinate(gx * size, gy * size),
          new Coordinate((gx + 1) * size, gy * size),
          new Coordinate((gx + 1) * size, (gy + 1) * size),
          new Coordinate(gx * size, (gy + 1) * size),
          new Coordinate(gx * size, gy * size)
        };

    return geometryFactory.createPolygon(coordinates);
  }

  private record CellIndex(int x, int y) {}

  @Builder(toBuilder = true)
  private record GridClassification(
      Set<CellIndex> toKeep, Set<CellIndex> invalidGrid, Set<CellIndex> toDelete) {}

  private record Grid(Map<CellIndex, Geometry> data) {
    Geometry get(CellIndex idx) {
      return data.get(idx);
    }

    void put(CellIndex idx, Geometry value) {
      data.put(idx, value);
    }
  }

  @Builder(toBuilder = true)
  public record Plane3DLongLineRemoverConf(
      double gridSize,
      double cellMin2DArea,
      double minAreaToCheck,
      double longlineLength,
      int cellMinNeighborsCount) {}
}
