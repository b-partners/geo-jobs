package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.*;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.repository.model.detection.Detection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionVGGUpdate implements BiFunction<VGG, Detection, Detection> {
  private static final String VGG_BUCKET_FOLDER = "vgg/";
  private final FileWriter fileWriter;
  private final BucketComponent bucketComponent;

  @Override
  public Detection apply(VGG vgg, Detection detection) {
    var zoneName = detection.getZoneName();
    var zoneDetectionJobId = detection.getZdjId();
    var fileKey = VGG_BUCKET_FOLDER + zoneDetectionJobId + "/" + zoneName + ".json";
    var vggAsByte = vgg.getBytes();
    var vggAsFile =
        fileWriter.write(vggAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
    bucketComponent.upload(vggAsFile, fileKey);
    return detection.toBuilder().vggFileKey(fileKey).build();
  }

  public Detection apply(Map<Feature, VGG> vgg, Detection detection) {
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    var layer = detection.getGeoServerProperties().getGeoServerParameter().getLayers();
    var updatedGeoJsonZone =
        providedGeoJsonZone.stream()
            .map(
                feature -> {
                  var optionalVgg =
                      vgg.entrySet().stream()
                          .filter(
                              entry -> {
                                try {
                                  // case for point
                                  if (feature.equals(entry.getKey())) return true;
                                  // case for polygon or multiPolygon
                                  if (entry.getKey().getProperties() == null) return false;
                                  if (entry.getKey().getProperties().get("centroid") == null)
                                    return false;
                                  var pointFromCentroidFeature =
                                      toRestFeature(
                                          new ObjectMapper()
                                              .readValue(
                                                  entry
                                                      .getKey()
                                                      .getProperties()
                                                      .get("centroid")
                                                      .toString(),
                                                  app.bpartners.geojobs.repository.model.Feature
                                                      .class));
                                  return feature.equals(pointFromCentroidFeature);
                                } catch (JsonProcessingException e) {
                                  throw new RuntimeException(e);
                                }
                              })
                          .findAny();

                  if (optionalVgg.isPresent()) {
                    var point = getPointOrCentroidAttribute(feature);
                    var longitude = point.getCoordinates().getFirst();
                    var latitude = point.getCoordinates().getLast();

                    var filename = layer + "/vgg_" + longitude + "_" + latitude;
                    var fileKey = filename + ".json";
                    var vggAsByte = optionalVgg.get().getValue().getBytes();
                    var vggAsFile = fileWriter.write(vggAsByte, createTempDirectory(), filename);
                    bucketComponent.upload(vggAsFile, fileKey);

                    var propertiesWithVggFileKey =
                        new HashMap<>(Objects.requireNonNull(feature.getProperties()));
                    propertiesWithVggFileKey.put("vgg_file_key", fileKey);
                    return new Feature()
                        .type(feature.getType())
                        .geometry(feature.getGeometry())
                        .properties(propertiesWithVggFileKey);
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .toList();

    return detection.toBuilder()
        .providedGeoJsonZone(
            updatedGeoJsonZone.stream().map(FeatureMapper::toDomainFeature).toList())
        .build();
  }
}
