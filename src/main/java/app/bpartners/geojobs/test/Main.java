package app.bpartners.geojobs.test;

import app.bpartners.geojobs.service.lidar.model.LidarClass;
import app.bpartners.geojobs.service.lidar.model.geometry.InclinedSurfaceSeparator;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGeometry;
import app.bpartners.geojobs.service.lidar.model.geometry.LasPointGroupedByX;
import java.io.FileWriter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
  public static void main(String[] args) {
    var points = four_plans_plus_bottom();
    var surfaces = new InclinedSurfaceSeparator().apply(points);

    log.info("surfaces.size={}", surfaces.size());
    for (int i = 0; i < surfaces.size(); i++) {
      var surface = surfaces.get(i);
      savePoints("/home/ricka/Lidar/Surfaces/surface" + i + ".geojson", surface.points());
    }
  }

  public static void mainx(String[] args) {
    test_group(roof_with_2_plans());
  }

  public static void test_group(Collection<LasPointGeometry> points) {
    var grouped =
        LasPointGroupedByX.from(
            points, InclinedSurfaceSeparator.InclinedSurfaceSeparatorConf.DEFAULT_EPSILON_X);

    int i = 1;
    log.info("group.size={}", grouped.groups().size());
    for (var group : grouped.groups()) {
      savePoints("/home/ricka/Lidar/Group/group" + i++ + ".geojson", group);
    }
  }

  public static List<LasPointGeometry> lambda_two_plans() {
    List<LasPointGeometry> allPoints = new ArrayList<>();

    int nExtraX = 10; // nombre de X supplémentaires
    int nPointsPerPlan = 50; // points par plan
    double xStep = 2; // écart entre chaque base X
    double yStep = 0.2; // espacement Y entre points
    double zStep = 0.01; // espacement Y entre points
    double zNoise = 0; // petit bruit Z
    double yNoise = 0; // petit bruit Y
    double xNoise = 0; // petit bruit X

    // Bases X (première base + xStep * i)
    double startX = 644493.1;
    double startY = 6858552.0;

    for (int xIdx = 0; xIdx < nExtraX; xIdx++) {
      double baseX = startX + xIdx * xStep;

      // --- Plan montant ---
      double plan1ZStart = 45.0;
      for (int i = 0; i < nPointsPerPlan; i++) {
        double x = baseX + Math.random() * xNoise;
        double y = startY + i * yStep + Math.random() * yNoise;
        double z = plan1ZStart + i * zStep + Math.random() * zNoise;
        allPoints.add(new LasPointGeometry(x, y, z, LidarClass.BATIMENT));
      }

      // --- Plan descendant ---
      double plan2ZStart = 48.0;
      double plan2YStart = startY + 8;
      for (int i = 0; i < nPointsPerPlan; i++) {
        double x = baseX + Math.random() * xNoise;
        double y = plan2YStart + i * yStep + Math.random() * yNoise;
        double z = plan2ZStart - i * zStep + Math.random() * zNoise;
        allPoints.add(new LasPointGeometry(x, y, z, LidarClass.BATIMENT));
      }
    }

    savePoints("lambda_two_plans.geojson", allPoints);
    return allPoints;
  }

  public static List<LasPointGeometry> four_plans_plus_bottom() {
    List<LasPointGeometry> allPoints = new ArrayList<>();

    int nExtraX = 10;
    int nPointsPerPlan = 50;
    double xStep = 2;
    double yStep = 0.2;
    double zNoise = 0;
    double yNoise = 0;
    double xNoise = 0;

    double startX = 644493.1;
    double startY = 6858552.0;

    for (int xIdx = 0; xIdx < nExtraX; xIdx++) {
      double baseX = startX + xIdx * xStep;

      // --- Plan bas plat (1er plan) ---
      double zBase = 45.0;
      for (int i = 0; i < nPointsPerPlan; i++) {
        double x = baseX + Math.random() * xNoise;
        double y = startY + i * yStep + Math.random() * yNoise;
        double z = zBase + Math.random() * zNoise;
        allPoints.add(new LasPointGeometry(x, y, z, LidarClass.BATIMENT));
      }

      // --- Plan montant (2ème plan) ---
      double zUpStart = zBase;
      double yUpStart = startY + nPointsPerPlan * yStep;
      double zStepUp = 0.6;
      for (int i = 0; i < nPointsPerPlan; i++) {
        double x = baseX + Math.random() * xNoise;
        double y = yUpStart + i * yStep + Math.random() * yNoise;
        double z = zUpStart + i * zStepUp + Math.random() * zNoise;
        allPoints.add(new LasPointGeometry(x, y, z, LidarClass.BATIMENT));
      }

      // --- Plan descendant (3ème plan) ---
      double zDownStart = zUpStart + nPointsPerPlan * zStepUp;
      double yDownStart = yUpStart + nPointsPerPlan * yStep;
      double zStepDown = -0.6;
      for (int i = 0; i < nPointsPerPlan; i++) {
        double x = baseX + Math.random() * xNoise;
        double y = yDownStart + i * yStep + Math.random() * yNoise;
        double z = zDownStart + i * zStepDown + Math.random() * zNoise;
        allPoints.add(new LasPointGeometry(x, y, z, LidarClass.BATIMENT));
      }

      // --- Plan supplémentaire (5ème plan) 5m plus bas ---
      double zLower = zBase - 5.0;
      double yLowerStart = startY; // commence au même Y que le premier plan
      int totalPointsY = nPointsPerPlan * 4; // longueur totale en Y des 4 plans précédents
      for (int i = 0; i < totalPointsY; i++) {
        double x = baseX + Math.random() * xNoise;
        double y = yLowerStart + i * yStep + Math.random() * yNoise;
        double z = zLower + Math.random() * zNoise;
        allPoints.add(new LasPointGeometry(x, y, z, LidarClass.BATIMENT));
      }
    }

    savePoints("four_plans_plus_bottom.geojson", allPoints);
    return allPoints;
  }

  public static List<LasPointGeometry> roof_with_2_plans() {
    List<LasPointGeometry> allPoints = new ArrayList<>();

    int nExtraX = 10; // longueur du toit
    int nPointsPerSlope = 50; // résolution par pente
    double xStep = 2; // pas en X
    double yStep = 0.2; // pas en Y
    double slopeZ = 0.8; // pente (beaucoup plus marquée)

    double zRidge = 50.0; // hauteur au faîte (centre du toit)
    double halfWidth = 5.0; // moitié de largeur du toit

    double startX = 644493.1;
    double startY = 6858552.0;

    for (int xIdx = 0; xIdx < nExtraX; xIdx++) {
      double baseX = startX + xIdx * xStep;

      // --- Côté gauche ---
      for (int i = 0; i < nPointsPerSlope; i++) {
        double y = startY - i * yStep; // côté gauche
        double distFromCenter = i * yStep;
        if (distFromCenter > halfWidth) break;
        double z = zRidge - (distFromCenter * slopeZ); // plus bas quand on s’éloigne
        allPoints.add(new LasPointGeometry(baseX, y, z, LidarClass.BATIMENT));
      }

      // --- Côté droit ---
      for (int i = 0; i < nPointsPerSlope; i++) {
        double y = startY + i * yStep; // côté droit
        double distFromCenter = i * yStep;
        if (distFromCenter > halfWidth) break;
        double z = zRidge - (distFromCenter * slopeZ);
        allPoints.add(new LasPointGeometry(baseX, y, z, LidarClass.BATIMENT));
      }
    }

    savePoints("roof_with_2_plans.geojson", allPoints);
    return allPoints;
  }

  public static void savePoints(String filename, Collection<LasPointGeometry> points) {
    StringBuilder sb = new StringBuilder();

    sb.append("{\n");
    sb.append("  \"type\": \"FeatureCollection\",\n");
    sb.append("  \"crs\": {\n");
    sb.append("    \"type\": \"name\",\n");
    sb.append("    \"properties\": {\n");
    sb.append("      \"name\": \"EPSG:2154\"\n");
    sb.append("    }\n");
    sb.append("  },\n");
    sb.append("  \"features\": [\n");

    boolean first = true;
    for (LasPointGeometry p : points) {
      if (!first) sb.append(",\n");
      first = false;

      double x = p.getX();
      double y = p.getY();
      double z = p.getCoordinate().getZ();

      sb.append(
          String.format(
              Locale.US,
              "    { \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\":"
                  + " [%.3f, %.3f, %.3f] }, \"properties\": {} }",
              x,
              y,
              z));
    }

    sb.append("\n  ]\n");
    sb.append("}\n");

    try (FileWriter writer = new FileWriter(filename)) {
      writer.write(sb.toString());
    } catch (Exception e) {
      throw new RuntimeException("Failed to save points to " + filename, e);
    }
  }
}
