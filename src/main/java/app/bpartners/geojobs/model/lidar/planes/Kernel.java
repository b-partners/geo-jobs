package app.bpartners.geojobs.model.lidar.planes;

import static java.util.Comparator.comparingDouble;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import java.security.SecureRandom;
import java.util.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class Kernel {
  private final int attempts;
  private final int maxNeighborsCount;
  private final double threshold;
  private final double minVectorNorm;
  private final double orthogonalDegEpsilon;

  @Getter private final List<LasPointGeometry> points;
  private static final double ORTHOGONAL_ANGLE = 90.0;

  public static Optional<Kernel> from(
      Collection<LasPointGeometry> points,
      SecureRandom random,
      int attempts,
      int maxNeighborsCount,
      double threshold,
      double minVectorNorm,
      double orthogonalDegEpsilon) {
    var kernelPoints =
        getOrthogonalTriplet(
            new ArrayList<>(points),
            random,
            attempts,
            maxNeighborsCount,
            threshold,
            minVectorNorm,
            orthogonalDegEpsilon);

    if (kernelPoints.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        Kernel.builder()
            .points(kernelPoints)
            .attempts(attempts)
            .threshold(threshold)
            .minVectorNorm(minVectorNorm)
            .maxNeighborsCount(maxNeighborsCount)
            .orthogonalDegEpsilon(orthogonalDegEpsilon)
            .build());
  }

  private static List<LasPointGeometry> getOrthogonalTriplet(
      List<LasPointGeometry> points,
      SecureRandom random,
      int attempts,
      int maxNeighborsCount,
      double threshold,
      double minVectorNorm,
      double orthogonalAngleDegEpsilon) {

    var maxAbsCosine = getMaxAbsCosine(orthogonalAngleDegEpsilon);
    for (int i = 0; i < attempts; i++) {
      var p1 = points.get(random.nextInt(points.size()));
      var candidates = getNeighborsCandidates(points, p1, threshold, minVectorNorm);

      if (candidates.size() < 3) {
        continue;
      }

      var p2Candidates = getP2Candidates(candidates, random, attempts, maxNeighborsCount);
      for (var p2Candidate : p2Candidates) {
        var p3Candidate = getP3Candidate(candidates, p2Candidate);
        if (p3Candidate.absCos() < maxAbsCosine) {
          return List.of(p1, p2Candidate.point(), p3Candidate.point());
        }
      }
    }
    return List.of();
  }

  private static double getMaxAbsCosine(double orthogonalAngleDegEpsilon) {
    return Math.cos(Math.toRadians(ORTHOGONAL_ANGLE - orthogonalAngleDegEpsilon));
  }

  private static List<Candidate> getNeighborsCandidates(
      List<LasPointGeometry> points, LasPointGeometry p1, double threshold, double minVectorNorm) {
    return points.stream()
        .filter(point -> point != p1 && p1.distance(point) < threshold)
        .map(point -> Candidate.from(p1, point))
        .filter(candidate -> candidate.norm() > minVectorNorm)
        .toList();
  }

  private static List<Candidate> getP2Candidates(
      List<Candidate> neighbors, SecureRandom random, int attempts, int maxNeighborsCount) {
    if (neighbors.size() <= maxNeighborsCount) {
      return neighbors;
    }
    return sampleRandomP2Candidates(neighbors, random, attempts);
  }

  private static List<Candidate> sampleRandomP2Candidates(
      List<Candidate> neighbors, SecureRandom random, int attempts) {
    Set<Candidate> selected = new HashSet<>();
    for (int i = 0; i < attempts; i++) {
      selected.add(neighbors.get(random.nextInt(neighbors.size())));
    }
    return new ArrayList<>(selected);
  }

  private static P3Candidate getP3Candidate(List<Candidate> candidates, Candidate p2Candidate) {
    return candidates.stream()
        .map(
            p3Candidate -> {
              if (p3Candidate == p2Candidate) return null;
              return P3Candidate.from(p2Candidate, p3Candidate);
            })
        .filter(Objects::nonNull)
        .min(comparingDouble(P3Candidate::absCos))
        .orElseThrow();
  }

  private record Candidate(LasPointGeometry point, Vector3D vector, double norm) {
    static Candidate from(LasPointGeometry p1, LasPointGeometry point) {
      var v1 = Vector3D.from(p1, point);
      var norm = v1.norm();
      return new Candidate(point, v1, norm);
    }
  }

  private record P3Candidate(Candidate candidate, double absCos) {
    LasPointGeometry point() {
      return this.candidate().point();
    }

    static P3Candidate from(Candidate p2Candidate, Candidate p3Candidate) {
      var v1 = p2Candidate.vector();
      var v2 = p3Candidate.vector();
      var n1 = p2Candidate.norm();
      var n2 = p3Candidate.norm();
      var absCos = Math.abs(v1.dot(v2) / (n1 * n2));

      return new P3Candidate(p3Candidate, absCos);
    }
  }
}
