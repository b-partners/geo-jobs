package app.bpartners.geojobs.service.cityjson;

import app.bpartners.geojobs.model.lidar.planes.algorithm.Vector3DUtils;
import app.bpartners.geojobs.service.cityjson.model.BuildingData;
import app.bpartners.geojobs.service.cityjson.model.CityJsonFile;
import app.bpartners.geojobs.service.cityjson.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.model.RowCol;
import app.bpartners.geojobs.service.cityjson.model.TextureFile;
import app.bpartners.geojobs.service.cityjson.model.TexturedBuildingData;
import app.bpartners.geojobs.service.cityjson.model.TexturedCityJson;
import app.bpartners.geojobs.service.cityjson.model.TexturedGeometry;
import app.bpartners.geojobs.service.cityjson.model.UV;
import app.bpartners.geojobs.service.lidar.model.geometry.GeometryWithProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.locationtech.jts.math.Vector3D;
import org.springframework.stereotype.Service;

@Service
public class CityJsonTextureService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public TexturedBuildingData texture(BuildingData buildingData, Path tifPath) throws IOException {
    RasterInfo rasterInfo = readRasterInfo(tifPath);
    String textureDataUri = imageToDataUri(tifPath);

    return TexturedBuildingData.builder()
        .id(buildingData.id())
        .roofs(textureGeometries(buildingData.roofs(), rasterInfo))
        .walls(textureGeometries(buildingData.walls(), rasterInfo))
        .grounds(textureGeometries(buildingData.grounds(), rasterInfo))
        .properties(buildingData.properties())
        .textureDataUri(textureDataUri)
        .build();
  }

  private List<TexturedGeometry> textureGeometries(
      List<GeometryWithProperties> geometries, RasterInfo rasterInfo) {
    return geometries.stream().map(g -> textureGeometry(g, rasterInfo)).toList();
  }

  private TexturedGeometry textureGeometry(
      GeometryWithProperties geometryWithProperties, RasterInfo rasterInfo) {
    var polygon = geometryWithProperties.asPolygon();
    var coordinates = polygon.getExteriorRing().getCoordinates();

    List<Vector3D> vertices =
        Arrays.stream(coordinates).map(c -> new Vector3D(c.getX(), c.getY(), c.getZ())).toList();

    List<UV> uvs = computeUv(vertices, rasterInfo);
    List<TexturedGeometry.UV> texturedUvs =
        uvs.stream().map(uv -> new TexturedGeometry.UV(uv.u(), uv.v())).toList();

    return new TexturedGeometry(
        geometryWithProperties.geometry(), geometryWithProperties.properties(), texturedUvs);
  }

  public void textureCityJson(Path cityJsonPath, Path tifPath, Path outputDirectory, int roofNumber)
      throws IOException {
    Files.createDirectories(outputDirectory);

    Path outputPath = outputDirectory.resolve("roof" + roofNumber + ".json");

    CityJsonFile cityJsonFile = loadCityJson(cityJsonPath);
    TextureFile textureFile = loadTexture(tifPath);

    TexturedCityJson texturedCityJson = texture(cityJsonFile, textureFile);

    save(texturedCityJson, outputPath);

    saveTexture(tifPath, outputDirectory);
  }

  public CityJsonFile loadCityJson(Path path) throws IOException {
    ObjectNode cityJson = (ObjectNode) objectMapper.readTree(path.toFile());

    ArrayNode rawVertices = (ArrayNode) cityJson.get("vertices");

    double scaleX = 1.0;
    double scaleY = 1.0;
    double scaleZ = 1.0;

    double translateX = 0.0;
    double translateY = 0.0;
    double translateZ = 0.0;

    if (cityJson.has("transform")) {
      JsonNode transform = cityJson.get("transform");

      JsonNode scale = transform.get("scale");
      JsonNode translate = transform.get("translate");

      scaleX = scale.get(0).asDouble();
      scaleY = scale.get(1).asDouble();
      scaleZ = scale.get(2).asDouble();

      translateX = translate.get(0).asDouble();
      translateY = translate.get(1).asDouble();
      translateZ = translate.get(2).asDouble();
    }

    List<Vector3D> vertices = new ArrayList<>();

    for (JsonNode rawVertex : rawVertices) {
      double x = rawVertex.get(0).asDouble() * scaleX + translateX;
      double y = rawVertex.get(1).asDouble() * scaleY + translateY;
      double z = rawVertex.get(2).asDouble() * scaleZ + translateZ;

      vertices.add(new Vector3D(x, y, z));
    }

    return new CityJsonFile(cityJson, vertices);
  }

  public TextureFile loadTexture(Path path) throws IOException {
    RasterInfo rasterInfo = readRasterInfo(path);
    String dataUri = imageToDataUri(path);
    return new TextureFile(dataUri, rasterInfo);
  }

  public TexturedCityJson texture(CityJsonFile cityJsonFile, TextureFile textureFile) {
    ObjectNode cityJson = cityJsonFile.json().deepCopy();
    List<Vector3D> vertices = cityJsonFile.vertices();

    RasterInfo rasterInfo = textureFile.rasterInfo();
    String textureDataUri = textureFile.dataUri();

    ObjectNode appearance = initAppearance(textureDataUri);

    List<UV> vertexTexture = new ArrayList<>();
    Map<String, Integer> vertexTextureMap = new HashMap<>();

    ObjectNode cityObjects = (ObjectNode) cityJson.get("CityObjects");
    Iterator<JsonNode> objects = cityObjects.elements();

    while (objects.hasNext()) {
      ObjectNode cityObject = (ObjectNode) objects.next();

      if (!"Building".equals(cityObject.get("type").asText())) {
        continue;
      }

      ArrayNode geometries = (ArrayNode) cityObject.get("geometry");

      for (JsonNode geometryNode : geometries) {
        ObjectNode geometry = (ObjectNode) geometryNode;

        ObjectNode geometryAppearance = initGeometryAppearance();
        geometry.set("appearance", geometryAppearance);

        List<JsonNode> faces = extractFaces(geometry);

        ArrayNode surfaces = getSemanticSurfaces(geometry);
        boolean hasSemantics = surfaces != null && surfaces.size() == faces.size();

        List<String> surfaceTypes = resolveSurfaceTypes(surfaces, faces.size(), hasSemantics);

        for (int i = 0; i < faces.size(); i++) {
          processFace(
              faces.get(i),
              surfaceTypes.get(i),
              hasSemantics,
              vertices,
              rasterInfo,
              vertexTexture,
              vertexTextureMap,
              geometryAppearance);
        }
      }
    }

    ArrayNode verticesTextureNode = objectMapper.createArrayNode();

    for (UV uv : vertexTexture) {
      ArrayNode uvNode = objectMapper.createArrayNode();
      uvNode.add(uv.u());
      uvNode.add(uv.v());
      verticesTextureNode.add(uvNode);
    }

    appearance.set("vertices-texture", verticesTextureNode);
    cityJson.set("appearance", appearance);

    return new TexturedCityJson(cityJson);
  }

  public void save(TexturedCityJson texturedCityJson, Path outputPath) throws IOException {
    objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValue(outputPath.toFile(), texturedCityJson.json());
  }

  public void buildTexturedCityJson(Path cityJsonPath, Path tifPath, Path outputPath)
      throws IOException {
    CityJsonFile cityJsonFile = loadCityJson(cityJsonPath);
    TextureFile textureFile = loadTexture(tifPath);

    TexturedCityJson texturedCityJson = texture(cityJsonFile, textureFile);

    save(texturedCityJson, outputPath);
  }

  public List<JsonNode> extractFaces(ObjectNode geometry) {
    String type = geometry.get("type").asText();
    ArrayNode boundaries = (ArrayNode) geometry.get("boundaries");

    List<JsonNode> faces = new ArrayList<>();

    if ("Solid".equals(type)) {
      for (JsonNode shell : boundaries) {
        for (JsonNode face : shell) {
          faces.add(face);
        }
      }
    } else {
      for (JsonNode face : boundaries) {
        faces.add(face);
      }
    }

    return faces;
  }

  public List<Vector3D> getFaceCoords(JsonNode face, List<Vector3D> vertices) {
    JsonNode outerRing = face.get(0);

    List<Vector3D> coords = new ArrayList<>();

    for (JsonNode vertexIndexNode : outerRing) {
      int vertexIndex = vertexIndexNode.asInt();
      coords.add(vertices.get(vertexIndex));
    }

    return coords;
  }

  public boolean isRoof(List<Vector3D> coords) {
    if (coords.size() < 3) {
      return false;
    }

    Vector3D p0 = coords.get(0);
    Vector3D p1 = coords.get(1);
    Vector3D p2 = coords.get(2);

    Vector3D v1 = new Vector3D(p1.getX() - p0.getX(), p1.getY() - p0.getY(), p1.getZ() - p0.getZ());
    Vector3D v2 = new Vector3D(p2.getX() - p0.getX(), p2.getY() - p0.getY(), p2.getZ() - p0.getZ());
    Vector3D normal = Vector3DUtils.cross(v1, v2);

    double unitZ = normal.getZ() / (normal.length() + 1e-12);

    return Math.abs(unitZ) > 0.7;
  }

  public boolean isRoofSemantic(String faceType) {
    return faceType != null && faceType.toLowerCase(Locale.ROOT).contains("roof");
  }

  public List<UV> computeUv(List<Vector3D> coords, RasterInfo rasterInfo) {
    List<UV> result = new ArrayList<>();

    for (Vector3D coord : coords) {
      RowCol rowCol = rowColAffine(rasterInfo, coord.getX(), coord.getY());

      double u = rowCol.col() / rasterInfo.width();
      double v = 1.0 - (rowCol.row() / rasterInfo.height());

      result.add(new UV(u, v));
    }

    return result;
  }

  public List<Integer> deduplicateUvs(
      List<UV> uv, List<UV> vertexTexture, Map<String, Integer> vertexTextureMap) {
    List<Integer> vtIndices = new ArrayList<>();

    for (UV coordinate : uv) {
      String key = "%.6f,%.6f".formatted(coordinate.u(), coordinate.v());

      Integer existingIndex = vertexTextureMap.get(key);

      if (existingIndex == null) {
        int newIndex = vertexTexture.size();
        vertexTextureMap.put(key, newIndex);
        vertexTexture.add(coordinate);
        vtIndices.add(newIndex);
      } else {
        vtIndices.add(existingIndex);
      }
    }

    return vtIndices;
  }

  public ObjectNode initAppearance(String textureDataUri) {
    ObjectNode appearance = objectMapper.createObjectNode();

    ArrayNode textures = objectMapper.createArrayNode();
    ObjectNode texture = objectMapper.createObjectNode();
    texture.put("type", "PNG");
    texture.put("image", textureDataUri);
    textures.add(texture);

    ArrayNode materials = objectMapper.createArrayNode();

    ObjectNode roofMaterial = objectMapper.createObjectNode();
    roofMaterial.put("name", "roof_material");
    roofMaterial.put("ambientIntensity", 1.0);
    materials.add(roofMaterial);

    ObjectNode wallMaterial = objectMapper.createObjectNode();
    wallMaterial.put("name", "wall_gray");

    ArrayNode diffuseColor = objectMapper.createArrayNode();
    diffuseColor.add(0.6);
    diffuseColor.add(0.6);
    diffuseColor.add(0.6);

    wallMaterial.set("diffuseColor", diffuseColor);
    materials.add(wallMaterial);

    appearance.set("textures", textures);
    appearance.set("materials", materials);
    appearance.set("vertices-texture", objectMapper.createArrayNode());

    return appearance;
  }

  public ObjectNode initGeometryAppearance() {
    ObjectNode geometryAppearance = objectMapper.createObjectNode();

    ObjectNode texture = objectMapper.createObjectNode();
    ObjectNode textureDefault = objectMapper.createObjectNode();
    textureDefault.set("values", objectMapper.createArrayNode());
    texture.set("default", textureDefault);

    ObjectNode material = objectMapper.createObjectNode();
    ObjectNode materialDefault = objectMapper.createObjectNode();
    materialDefault.set("values", objectMapper.createArrayNode());
    material.set("default", materialDefault);

    geometryAppearance.set("texture", texture);
    geometryAppearance.set("material", material);

    return geometryAppearance;
  }

  public void processFace(
      JsonNode face,
      String faceType,
      boolean hasSemantics,
      List<Vector3D> vertices,
      RasterInfo rasterInfo,
      List<UV> vertexTexture,
      Map<String, Integer> vertexTextureMap,
      ObjectNode geometryAppearance) {
    List<Vector3D> coords = getFaceCoords(face, vertices);

    if (coords.size() < 3) {
      return;
    }

    List<UV> uv = computeUv(coords, rasterInfo);

    List<Integer> vtIndices = deduplicateUvs(uv, vertexTexture, vertexTextureMap);

    boolean roof = hasSemantics ? isRoofSemantic(faceType) : isRoof(coords);

    ArrayNode textureValues =
        (ArrayNode) geometryAppearance.get("texture").get("default").get("values");

    ArrayNode materialValues =
        (ArrayNode) geometryAppearance.get("material").get("default").get("values");

    if (roof) {
      ArrayNode ringTextureIndices = objectMapper.createArrayNode();

      for (Integer index : vtIndices) {
        ringTextureIndices.add(index);
      }

      ArrayNode faceTextureIndices = objectMapper.createArrayNode();
      faceTextureIndices.add(ringTextureIndices);

      textureValues.add(faceTextureIndices);
      materialValues.add(0);
    } else {
      textureValues.addNull();

      if (!hasSemantics) {
        materialValues.add(1);
      }
    }
  }

  private String imageToDataUri(Path tifPath) throws IOException {
    BufferedImage image = ImageIO.read(tifPath.toFile());
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", baos);
      byte[] imageBytes = baos.toByteArray();
      String base64Image = Base64.getEncoder().encodeToString(imageBytes);
      return "data:image/png;base64," + base64Image;
    }
  }

  private String saveTexture(Path tifPath, Path outputDirectory) throws IOException {
    Files.createDirectories(outputDirectory);

    BufferedImage image = ImageIO.read(tifPath.toFile());

    if (image == null) {
      throw new IOException("Could not read GeoTIFF image: " + tifPath);
    }

    BufferedImage rgbImage =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

    Graphics2D graphics = rgbImage.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();

    Path outputPath = outputDirectory.resolve("texture.png");

    ImageIO.write(rgbImage, "png", outputPath.toFile());

    return outputPath.toAbsolutePath().toString();
  }

  private ArrayNode getSemanticSurfaces(ObjectNode geometry) {
    if (!geometry.has("semantics")) {
      return null;
    }

    JsonNode semantics = geometry.get("semantics");

    if (!semantics.has("surfaces")) {
      return null;
    }

    return (ArrayNode) semantics.get("surfaces");
  }

  private List<String> resolveSurfaceTypes(
      ArrayNode surfaces, int faceCount, boolean hasSemantics) {
    List<String> surfaceTypes = new ArrayList<>();

    if (hasSemantics) {
      for (JsonNode surface : surfaces) {
        String type = surface.has("type") ? surface.get("type").asText() : "WallSurface";

        surfaceTypes.add(type);
      }
    } else {
      for (int i = 0; i < faceCount; i++) {
        surfaceTypes.add(null);
      }
    }

    return surfaceTypes;
  }

  private RasterInfo readRasterInfo(Path tifPath) throws IOException {
    BufferedImage image = ImageIO.read(tifPath.toFile());

    if (image == null) {
      throw new IOException("Could not read raster dimensions: " + tifPath);
    }

    double originX = 0.0;
    double originY = 0.0;
    double pixelWidth = 1.0;
    double pixelHeight = -1.0;

    try {
      Process process = new ProcessBuilder("gdalinfo", "-json", tifPath.toString()).start();
      com.fasterxml.jackson.databind.ObjectMapper mapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode gdalJson = mapper.readTree(process.getInputStream());
      if (gdalJson.has("geoTransform")) {
        com.fasterxml.jackson.databind.JsonNode gt = gdalJson.get("geoTransform");
        originX = gt.get(0).asDouble();
        pixelWidth = gt.get(1).asDouble();
        originY = gt.get(3).asDouble();
        pixelHeight = gt.get(5).asDouble();
      }
    } catch (Exception e) {
      System.err.println(
          "Warning: Could not read GeoTIFF transform using gdalinfo: " + e.getMessage());
    }

    return new RasterInfo(
        originX, originY, pixelWidth, pixelHeight, 0.0, 0.0, image.getWidth(), image.getHeight());
  }

  private RowCol rowColAffine(RasterInfo t, double x, double y) {
    double a = t.pixelWidth();
    double b = t.shearX();
    double c = t.originX();

    double d = t.shearY();
    double e = t.pixelHeight();
    double f = t.originY();

    double determinant = a * e - b * d;

    if (Math.abs(determinant) < 1e-12) {
      throw new IllegalArgumentException("Raster transform is not invertible");
    }

    double dx = x - c;
    double dy = y - f;

    double col = (e * dx - b * dy) / determinant;
    double row = (-d * dx + a * dy) / determinant;

    return new RowCol(row, col);
  }
}
