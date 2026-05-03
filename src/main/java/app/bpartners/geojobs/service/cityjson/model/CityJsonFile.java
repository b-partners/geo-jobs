package app.bpartners.geojobs.service.cityjson.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.locationtech.jts.math.Vector3D;

public record CityJsonFile(ObjectNode json, List<Vector3D> vertices) {}
