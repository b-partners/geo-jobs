package app.bpartners.geojobs.service.cityjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

public class CityJsonTextureService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void textureCityJson(
            Path cityJsonPath,
            Path tifPath,
            Path outputDirectory,
            int roofNumber
    ) throws IOException {
        Files.createDirectories(outputDirectory);

        String texturePath = saveTexture(tifPath, outputDirectory);

        Path outputPath = outputDirectory.resolve("roof" + roofNumber + ".json");

        buildTexturedCityJson(
                cityJsonPath,
                tifPath,
                texturePath,
                outputPath
        );
    }

    public LoadedCityJson loadCityJson(Path path) throws IOException {
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

        List<Vec3> vertices = new ArrayList<>();

        for (JsonNode rawVertex : rawVertices) {
            double x = rawVertex.get(0).asDouble() * scaleX + translateX;
            double y = rawVertex.get(1).asDouble() * scaleY + translateY;
            double z = rawVertex.get(2).asDouble() * scaleZ + translateZ;

            vertices.add(new Vec3(x, y, z));
        }

        return new LoadedCityJson(cityJson, vertices);
    }

    public void buildTexturedCityJson(
            Path cityJsonPath,
            Path tifPath,
            String texturePath,
            Path outputPath
    ) throws IOException {
        LoadedCityJson loadedCityJson = loadCityJson(cityJsonPath);

        ObjectNode cityJson = loadedCityJson.json();
        List<Vec3> vertices = loadedCityJson.vertices();

        RasterInfo rasterInfo = readRasterInfo(tifPath);

        ObjectNode appearance = initAppearance(texturePath);

        List<Vec2> vertexTexture = new ArrayList<>();
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
                            geometryAppearance
                    );
                }
            }
        }

        ArrayNode verticesTextureNode = objectMapper.createArrayNode();

        for (Vec2 uv : vertexTexture) {
            ArrayNode uvNode = objectMapper.createArrayNode();
            uvNode.add(uv.u());
            uvNode.add(uv.v());
            verticesTextureNode.add(uvNode);
        }

        appearance.set("vertices-texture", verticesTextureNode);
        cityJson.set("appearance", appearance);

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), cityJson);
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

    public List<Vec3> getFaceCoords(JsonNode face, List<Vec3> vertices) {
        JsonNode outerRing = face.get(0);

        List<Vec3> coords = new ArrayList<>();

        for (JsonNode vertexIndexNode : outerRing) {
            int vertexIndex = vertexIndexNode.asInt();
            coords.add(vertices.get(vertexIndex));
        }

        return coords;
    }

    public boolean isRoof(List<Vec3> coords) {
        if (coords.size() < 3) {
            return false;
        }

        Vec3 p0 = coords.get(0);
        Vec3 p1 = coords.get(1);
        Vec3 p2 = coords.get(2);

        Vec3 v1 = subtract(p1, p0);
        Vec3 v2 = subtract(p2, p0);
        Vec3 normal = cross(v1, v2);

        double norm = Math.sqrt(
                normal.x() * normal.x()
                        + normal.y() * normal.y()
                        + normal.z() * normal.z()
        );

        double unitZ = normal.z() / (norm + 1e-12);

        return Math.abs(unitZ) > 0.7;
    }

    public boolean isRoofSemantic(String faceType) {
        return faceType != null && faceType.toLowerCase(Locale.ROOT).contains("roof");
    }

    public List<Vec2> computeUv(List<Vec3> coords, RasterInfo rasterInfo) {
        List<Vec2> result = new ArrayList<>();

        for (Vec3 coord : coords) {
            RowCol rowCol = rowColAffine(rasterInfo, coord.x(), coord.y());

            double u = (double) rowCol.col() / rasterInfo.width();
            double v = 1.0 - ((double) rowCol.row() / rasterInfo.height());

            result.add(new Vec2(u, v));
        }

        return result;
    }

    public List<Integer> deduplicateUvs(
            List<Vec2> uv,
            List<Vec2> vertexTexture,
            Map<String, Integer> vertexTextureMap
    ) {
        List<Integer> vtIndices = new ArrayList<>();

        for (Vec2 coordinate : uv) {
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

    public ObjectNode initAppearance(String texturePath) {
        ObjectNode appearance = objectMapper.createObjectNode();

        ArrayNode textures = objectMapper.createArrayNode();
        ObjectNode texture = objectMapper.createObjectNode();
        texture.put("type", "PNG");
        texture.put("image", texturePath);
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
            List<Vec3> vertices,
            RasterInfo rasterInfo,
            List<Vec2> vertexTexture,
            Map<String, Integer> vertexTextureMap,
            ObjectNode geometryAppearance
    ) {
        List<Vec3> coords = getFaceCoords(face, vertices);

        if (coords.size() < 3) {
            return;
        }

        List<Vec2> uv = computeUv(coords, rasterInfo);

        List<Integer> vtIndices = deduplicateUvs(
                uv,
                vertexTexture,
                vertexTextureMap
        );

        boolean roof = hasSemantics
                ? isRoofSemantic(faceType)
                : isRoof(coords);

        ArrayNode textureValues = (ArrayNode) geometryAppearance
                .get("texture")
                .get("default")
                .get("values");

        ArrayNode materialValues = (ArrayNode) geometryAppearance
                .get("material")
                .get("default")
                .get("values");

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

    public String saveTexture(Path tifPath, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);

        BufferedImage image = ImageIO.read(tifPath.toFile());

        if (image == null) {
            throw new IOException("Could not read GeoTIFF image: " + tifPath);
        }

        BufferedImage rgbImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

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
            ArrayNode surfaces,
            int faceCount,
            boolean hasSemantics
    ) {
        List<String> surfaceTypes = new ArrayList<>();

        if (hasSemantics) {
            for (JsonNode surface : surfaces) {
                String type = surface.has("type")
                        ? surface.get("type").asText()
                        : "WallSurface";

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
            JsonNode gdalJson = objectMapper.readTree(process.getInputStream());
            if (gdalJson.has("geoTransform")) {
                JsonNode gt = gdalJson.get("geoTransform");
                originX = gt.get(0).asDouble();
                pixelWidth = gt.get(1).asDouble();
                originY = gt.get(3).asDouble();
                pixelHeight = gt.get(5).asDouble();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not read GeoTIFF transform using gdalinfo: " + e.getMessage());
        }

        return new RasterInfo(
                originX,
                originY,
                pixelWidth,
                pixelHeight,
                0.0,
                0.0,
                image.getWidth(),
                image.getHeight()
        );
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

    private Vec3 subtract(Vec3 a, Vec3 b) {
        return new Vec3(
                a.x() - b.x(),
                a.y() - b.y(),
                a.z() - b.z()
        );
    }

    private Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(
                a.y() * b.z() - a.z() * b.y(),
                a.z() * b.x() - a.x() * b.z(),
                a.x() * b.y() - a.y() * b.x()
        );
    }

    public record LoadedCityJson(ObjectNode json, List<Vec3> vertices) {}

    public record Vec3(double x, double y, double z) {}

    public record Vec2(double u, double v) {}

    public record RowCol(double row, double col) {}

    public record RasterInfo(
            double originX,
            double originY,
            double pixelWidth,
            double pixelHeight,
            double shearX,
            double shearY,
            int width,
            int height
    ) {}
}
