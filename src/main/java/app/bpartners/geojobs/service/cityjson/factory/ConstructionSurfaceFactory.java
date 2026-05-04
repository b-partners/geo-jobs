package app.bpartners.geojobs.service.cityjson.factory;

import app.bpartners.geojobs.service.cityjson.texture.model.ConstructionSurfaceType;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedGeometry;
import app.bpartners.geojobs.service.lidar.model.geometry.GeometryWithProperties;
import java.util.Arrays;
import java.util.List;
import org.citygml4j.cityjson.model.geometry.Vertex;
import org.citygml4j.core.model.construction.*;
import org.xmlobjects.gml.model.geometry.DirectPositionList;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurface;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurfaceProperty;
import org.xmlobjects.gml.model.geometry.primitives.LinearRing;
import org.xmlobjects.gml.model.geometry.primitives.Polygon;
import org.xmlobjects.gml.model.geometry.primitives.SurfaceProperty;

public class ConstructionSurfaceFactory {
  private static final int CRS_DIMENSION = 3;

  private ConstructionSurfaceFactory() {}

  public static AbstractConstructionSurface make(
      ConstructionSurfaceType type, GeometryWithProperties geometryWithProperties) {
    var vertices = toVertices(geometryWithProperties.asPolygon());
    var surface = buildSurface(type, vertices);

    var properties = GenericAttributeFactory.make(geometryWithProperties.properties());
    surface.setGenericAttributes(properties);

    return surface;
  }

  public static AbstractConstructionSurface make(
      ConstructionSurfaceType type, TexturedGeometry texturedGeometry) {
    var vertices = toVertices(texturedGeometry.geometry());
    var surface = buildSurface(type, vertices);

    var properties = GenericAttributeFactory.make(texturedGeometry.properties());
    surface.setGenericAttributes(properties);

    // TODO: Handle UVs for citygml4j CityJSON writer
    // This part is complex because citygml4j's CityJSON writer expects appearances
    // to be in the CityModel's appearance member, which is not easily accessible here.

    return surface;
  }

  private static AbstractConstructionSurface buildSurface(
      ConstructionSurfaceType type, List<Vertex> vertices) {
    var surface = getSurface(type);
    var ring = new LinearRing(toDirectionPositionList(vertices));
    var polygon = new Polygon(ring);

    var multiSurface = new MultiSurface();
    multiSurface.setSurfaceMember(List.of(new SurfaceProperty(polygon)));
    surface.setLod2MultiSurface(new MultiSurfaceProperty(multiSurface));

    return surface;
  }

  private static AbstractConstructionSurface getSurface(ConstructionSurfaceType type) {
    return switch (type) {
      case ROOF -> new RoofSurface();
      case WALL -> new WallSurface();
      case GROUND -> new GroundSurface();
    };
  }

  private static List<Vertex> toVertices(org.locationtech.jts.geom.Geometry geometry) {
    return Arrays.stream(geometry.getCoordinates())
        .map(coordinate -> Vertex.of(coordinate.getX(), coordinate.getY(), coordinate.getZ()))
        .toList();
  }

  private static List<Vertex> toVertices(org.locationtech.jts.geom.Polygon polygon) {
    return toVertices((org.locationtech.jts.geom.Geometry) polygon);
  }

  public static DirectPositionList toDirectionPositionList(List<Vertex> vertices) {
    var list =
        new DirectPositionList(
            vertices.stream()
                .map(vertex -> List.of(vertex.getX(), vertex.getY(), vertex.getZ()))
                .flatMap(List::stream)
                .toList());

    list.setSrsDimension(CRS_DIMENSION);
    return list;
  }
}
