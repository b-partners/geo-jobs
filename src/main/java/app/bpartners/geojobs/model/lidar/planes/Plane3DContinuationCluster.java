package app.bpartners.geojobs.model.lidar.planes;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.util.*;
import java.util.function.Function;

/**
 * Performs 3D clustering of points in a Plane3D based on a maximum distance. Points within the
 * given radius are connected. The largest connected cluster is returned as the main cluster, while
 * other points are considered outliers.
 */
public class Plane3DContinuationCluster
    implements Function<Plane3D, Plane3DContinuationCluster.Result> {
  private final double radius;
  private final int minClusterSize;

  /**
   * @param radius maximum distance to consider points connected U
   * @param minClusterSize minimum number of points to consider a valid cluster
   */
  public Plane3DContinuationCluster(double radius, int minClusterSize) {
    this.radius = radius;
    this.minClusterSize = minClusterSize;
  }

  @Override
  public Result apply(Plane3D plane) {
    var points = new ArrayList<>(plane.getPoints());
    int n = points.size();
    if (n == 0) {
      return new Result(Plane3D.empty(), List.of());
    }

    // Create a 3D spatial grid for faster neighbor search
    var grid = createGrid(points);

    // Connect points within the radius using Union-Find
    var unionFind = connectConnectedPoints(n, grid, points);

    // Count the size of each cluster
    var clusterSize = getClusterSize(n, unionFind);

    // Find the largest cluster
    int bestRoot = -1;
    int bestSize = -1;
    for (var e : clusterSize.entrySet()) {
      if (e.getValue() > bestSize) {
        bestSize = e.getValue();
        bestRoot = e.getKey();
      }
    }

    // If no cluster is large enough, return empty
    if (bestSize < minClusterSize) {
      return new Result(Plane3D.empty(), List.of());
    }

    // Separate points into the main cluster and outliers
    Set<LasPointGeometry> in = new HashSet<>();
    List<LasPointGeometry> out = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      if (unionFind.find(i) == bestRoot) {
        in.add(points.get(i));
      } else out.add(points.get(i));
    }

    return new Result(plane.with(in), out);
  }

  /** Count the size of each cluster */
  private Map<Integer, Integer> getClusterSize(
      int pointsSize, UnionFindLasPointGeometry unionFind) {
    Map<Integer, Integer> count = new HashMap<>();
    for (int i = 0; i < pointsSize; i++) {
      int root = unionFind.find(i);
      count.put(root, count.getOrDefault(root, 0) + 1);
    }
    return count;
  }

  /** Create a 3D spatial grid for faster neighbor search */
  private Map<CellIndex, List<Integer>> createGrid(List<LasPointGeometry> points) {
    Map<CellIndex, List<Integer>> grid = new HashMap<>();
    for (int i = 0; i < points.size(); i++) {
      var p = points.get(i);
      int ix = (int) Math.floor(p.getX() / radius);
      int iy = (int) Math.floor(p.getY() / radius);
      int iz = (int) Math.floor(p.getZ() / radius);
      grid.computeIfAbsent(new CellIndex(ix, iy, iz), k -> new ArrayList<>()).add(i);
    }
    return grid;
  }

  // Connect points within the radius using Union-Find
  private UnionFindLasPointGeometry connectConnectedPoints(
      int pointSize, Map<CellIndex, List<Integer>> grid, List<LasPointGeometry> points) {
    var unionFind = new UnionFindLasPointGeometry(pointSize);

    for (int i = 0; i < pointSize; i++) {
      var p = points.get(i);
      int ix = (int) Math.floor(p.getX() / radius);
      int iy = (int) Math.floor(p.getY() / radius);
      int iz = (int) Math.floor(p.getZ() / radius);

      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          for (int dz = -1; dz <= 1; dz++) {
            var cell = new CellIndex(ix + dx, iy + dy, iz + dz);
            var neigh = grid.get(cell);
            if (neigh == null) {
              continue;
            }

            var left = points.get(i);
            for (int j : neigh) {
              if (j <= i) {
                continue;
              }

              if (left.distance(points.get(j)) <= radius) {
                unionFind.union(i, j);
              }
            }
          }
        }
      }
    }

    return unionFind;
  }

  private record CellIndex(int x, int y, int z) {}

  public record Result(Plane3D plane, List<LasPointGeometry> outliers) {}
}
