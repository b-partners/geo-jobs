package app.bpartners.geojobs.service.PolygonContinue;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.consumer.PolygonFusionRequested;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.io.FileOutputStream;
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
    private final EventProducer<PolygonFusionRequested> eventProducer;

    /**
     * Point d'entrée asynchrone pour la fusion des polygones.
     */
    public Map<String, String> fusionnerPolygonesAsync(MultipartFile file) {
        try {
            File inputFile = convertMultipartFileToFile(file);
            eventProducer.accept(List.of(new PolygonFusionRequested(inputFile)));

            List<Polygon> validPolygons = loadAndValidatePolygons(inputFile);
            Geometry resultGeometry = mergePolygonsIfNeeded(validPolygons);
            File outputFile = saveGeometryAsGeoJson(resultGeometry);
            Map<String, String> result = new HashMap<>(uploadAndGetUrl(outputFile));
            result.put("localPath", outputFile.getAbsolutePath());
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fusion polygon", e);
        }
    }

    /** Convertit un MultipartFile en fichier temporaire. */
    private File convertMultipartFileToFile(MultipartFile multipart) {
        String uuidName = UUID.randomUUID().toString();
        try {
            File tempFile = File.createTempFile("fusion-input-" + uuidName, ".geojson");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(multipart.getBytes());
            }
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert MultipartFile to File", e);
        }
    }

    /** Transforme le contenu d'un fichier GeoJSON en une liste de Polygon. */
    @SneakyThrows
    private List<Polygon> loadAndValidatePolygons(File inputFile) {
        Set<LatLonPolygon> polygons = geoJsonLoader.apply(inputFile);

        List<Polygon> validPolygons = polygons.stream()
                .map(LatLonPolygon::polygon)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (validPolygons.isEmpty()) {
            throw new IllegalArgumentException("Impossible de transformer le fichier en une liste de polygones valides");
        }
        return validPolygons;
    }

    /** Détermine si les polygones doivent être fusionnés et retourne la géométrie finale. */
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

    /** Sauvegarde la géométrie fusionnée au format GeoJSON. */
    @SneakyThrows
    private File saveGeometryAsGeoJson(Geometry geometry) {
        Set<LatLonPolygon> mergedPolygons = new HashSet<>();

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
    }

    /** Upload le fichier vers S3 et retourne l'URL présignée. */
    private Map<String, String> uploadAndGetUrl(File file) {
        String bucketKey = file.getName();
        bucketComponent.upload(file, bucketKey);
        String presignedUrl = bucketComponent.presign(bucketKey);
        log.info("Fichier fusionné uploadé avec URL : {}", presignedUrl);
        return Map.of("url", presignedUrl);
    }
}
