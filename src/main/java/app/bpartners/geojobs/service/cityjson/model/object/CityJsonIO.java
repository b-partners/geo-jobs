package app.bpartners.geojobs.service.cityjson.model.object;

import app.bpartners.geojobs.service.cityjson.model.object.io.CityJsonVisitor;
import app.bpartners.geojobs.service.cityjson.model.object.io.SurfaceAnnotators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.*;
import lombok.SneakyThrows;

public class CityJsonIO {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT); // une ligne par objet

  public static class Doc {
    public CityJSON header;
    public List<CityJSONFeature> features = new ArrayList<>();
  }

  public static Doc read(Path path) throws Exception {
    Doc doc = new Doc();
    try (var lines = Files.lines(path)) {
      List<String> all = lines.filter(s -> !s.isBlank()).collect(Collectors.toList());
      doc.header = MAPPER.readValue(all.get(0), CityJSON.class);
      for (int i = 1; i < all.size(); i++) {
        doc.features.add(MAPPER.readValue(all.get(i), CityJSONFeature.class));
      }
    }
    return doc;
  }

  @SneakyThrows
  public static void write(Doc doc, Path path) {
    List<String> out = new ArrayList<>();
    out.add(MAPPER.writeValueAsString(doc.header));
    for (CityJSONFeature f : doc.features) {
      out.add(MAPPER.writeValueAsString(f));
    }
    Files.write(path, out);
  }

  @SneakyThrows
  public static CityJsonIO.Doc computeAdditionalProperties(File originalCityJson) {
    CityJsonIO.Doc actual = CityJsonIO.read(originalCityJson.toPath());

    Consumer<CityJsonVisitor.SurfaceContext> pipeline =
        SurfaceAnnotators.slopeInDegrees()
            .andThen(SurfaceAnnotators.onlyOn("RoofSurface", SurfaceAnnotators.areaM2()))
            .andThen(SurfaceAnnotators.onlyOn("WallSurface", SurfaceAnnotators.areaM2()))
            .andThen(SurfaceAnnotators.wallHeightInMeters());

    CityJsonVisitor.forEachSurface(actual, pipeline);

    return actual;
  }
}
