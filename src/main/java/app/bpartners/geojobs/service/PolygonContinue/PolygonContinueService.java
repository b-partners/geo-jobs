package app.bpartners.geojobs.service.PolygonContinue;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.consumer.PolygonContinueRequested;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import app.bpartners.geojobs.repository.PolygonContinueRepository;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
@Slf4j
public class PolygonContinueService {
    private final GeometryConverter geometryConverter;
    private final GeoJsonLoader geoJsonLoader;
    private final BucketComponent bucketComponent;
    private final EventProducer<PolygonContinueRequested> eventProducer;
    private final PolygonContinueRepository repository;

    public Map<String, String> PolygonsContinueAsync(MultipartFile file) {
        try {
            File inputFile = convertMultipartFileToFile(file);
            String bucketKey = generateBucketKey(inputFile);
            var polygonContinueGeoJson = repository.findById(bucketKey);

            if (polygonContinueGeoJson.isPresent()) {
                return uploadAndGetUrl(inputFile);
            }
            eventProducer.accept(List.of(new PolygonContinueRequested(inputFile)));

            List<Polygon> validPolygons = loadAndValidatePolygons(inputFile);
            Geometry resultGeometry = mergePolygonsIfNeeded(validPolygons);
            File outputFile = saveGeometryAsGeoJson(resultGeometry);
            Map<String, String> result = new HashMap<>(uploadAndGetUrl(outputFile));
            result.put("localPath", outputFile.getAbsolutePath());
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to continue polygons", e);
        }
    }

    private File convertMultipartFileToFile(MultipartFile multipart) {
        String uuidName = UUID.randomUUID().toString();
        try {
            File tempFile = File.createTempFile("polygons-input-" + uuidName, ".geojson");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(multipart.getBytes());
            }
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert MultipartFile to File", e);
        }
    }

    @SneakyThrows
    private List<Polygon> loadAndValidatePolygons(File inputFile) {
        Set<LatLonPolygon> polygons = geoJsonLoader.apply(inputFile);

        List<Polygon> validPolygons = polygons.stream()
                .map(LatLonPolygon::polygon)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (validPolygons.isEmpty()) {
            throw new IllegalArgumentException("Unable to convert the file into a list of valid polygons");
        }
        return validPolygons;
    }

    private Geometry mergePolygonsIfNeeded(List<Polygon> validPolygons) {
        if (shouldMergePolygons(validPolygons)) {
            return mergePolygons(validPolygons);
        } else {
            return geometryConverter.getGeometryFactory()
                    .createMultiPolygon(validPolygons.toArray(new Polygon[0]));
        }
    }

    private boolean shouldMergePolygons(List<Polygon> polygons) {
        if (polygons.size() <= 1) return false;
        double thresholdDistance = 0.0005; // ~50m
        return polygons.get(0).distance(polygons.get(1)) < thresholdDistance;
    }

    private Geometry mergePolygons(List<Polygon> polygons) {
        MultiPolygonUnion unionOperator = new MultiPolygonUnion();
        MultiPolygon union = geometryConverter.getGeometryFactory().createMultiPolygon(new Polygon[0]);

        for (Polygon polygon : polygons) {
            union = unionOperator.apply(union, polygon);
        }
        return (union.getNumGeometries() > 1) ? union.convexHull() : union;
    }

    private File saveGeometryAsGeoJson(Geometry geometry) {
        Set<LatLonPolygon> mergedPolygons = new HashSet<>();
        try {
            if (geometry instanceof MultiPolygon multi) {
                for (int i = 0; i < multi.getNumGeometries(); i++) {
                    mergedPolygons.add(new LatLonPolygon((Polygon) multi.getGeometryN(i)));
                }
            } else if (geometry instanceof Polygon poly) {
                mergedPolygons.add(new LatLonPolygon(poly));
            }

            Geojson geojson = new Geojson(mergedPolygons);
            File outputFile = File.createTempFile("merged", ".geojson");
            geojson.saveAsFile(outputFile.getAbsolutePath());
            return outputFile;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save geometry as GeoJSON", e);
        }
    }

    private String generateBucketKey(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            String fullHash = HexFormat.of().formatHex(digest.digest());
            return fullHash.substring(0, 16);
        } catch (Exception e) {
            throw new RuntimeException("failed to generate bucketKey", e);
        }
    }


    private Map<String, String> uploadAndGetUrl(File file) {
        String bucket = "polygon/bucket:"+ generateBucketKey(file) +"/"+ file.getName();
        bucketComponent.upload(file, bucket);
        String presignedUrl = bucketComponent.presign(bucket);
        log.info("Polygon continue file successfully uploaded. URL: {}", presignedUrl);
        return Map.of("url", presignedUrl);
    }
}
