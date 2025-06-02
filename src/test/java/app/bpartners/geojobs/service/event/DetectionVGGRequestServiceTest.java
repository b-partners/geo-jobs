package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_CLAIR;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionVGGRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.PolygonObjectTypeSerializable;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygonSerializable;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DetectionVGGUpdate;
import app.bpartners.geojobs.service.geojson.GeometryConverter;

import java.util.*;

import org.junit.jupiter.api.Test;

public class DetectionVGGRequestServiceTest {
    DetectionRepository detectionRepositoryMock = mock();
    VGGFactory vggFactory = mock();
    DetectionVGGUpdate detectionVGGUpdate = mock();
    GeometryConverter geometryConverter = mock();
    DetectionVGGRequestedService subject =
            new DetectionVGGRequestedService(
                    detectionRepositoryMock, vggFactory, detectionVGGUpdate, geometryConverter);
    @Test
    void throws_when_detection_not_found() {
        var eventMock = mock(DetectionVGGRequested.class);
        when(detectionRepositoryMock.findById(any())).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> {
                    subject.accept(eventMock);
                });
    }

    @Test
    void accept_detection_vgg_request_service() {
        var detectionId = randomUUID().toString();
        var detectionMock = mock(Detection.class);
        var filteredTiledPixelPolygons = mock(TiledPixelPolygonSerializable.class);
        var eventMock = mock(DetectionVGGRequested.class);
        var polygonObjectTypeSerializableMock = mock(PolygonObjectTypeSerializable.class);
        var jtsPolygonResultMock = mock(org.locationtech.jts.geom.Polygon.class);
        var featurePointMock = mock(Feature.class);
        var featureVggResultMock = mock(Feature.class);
        var vggResultMock = mock(VGG.class);
        Map<Feature, VGG> mapVggFactory = new HashMap<>();
        var newDetectionAfterVggUpdateMock = mock(Detection.class);
        var mockPolygonAsString = "POLYGON ((10.0 10.0))";

        when(detectionMock.getId()).thenReturn(detectionId);
        when(eventMock.getDetectionId()).thenReturn(detectionId);
        when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));
        when(eventMock.getFilteredTiledPixelPolygons()).thenReturn(List.of(filteredTiledPixelPolygons));
        when(filteredTiledPixelPolygons.polygons())
                .thenReturn(List.of(polygonObjectTypeSerializableMock));

        when(polygonObjectTypeSerializableMock.polygonAsString()).thenReturn(mockPolygonAsString);
        when(geometryConverter.readGeometryFromString(eq(mockPolygonAsString)))
                .thenReturn(jtsPolygonResultMock);
        when(polygonObjectTypeSerializableMock.detectableType()).thenReturn(MOISISSURE_CLAIR);

        when(filteredTiledPixelPolygons.point()).thenReturn(featurePointMock);
        when(filteredTiledPixelPolygons.tileX()).thenReturn(1);
        when(filteredTiledPixelPolygons.tileY()).thenReturn(2);
        when(filteredTiledPixelPolygons.zoom()).thenReturn(20);

        mapVggFactory.put(featureVggResultMock, vggResultMock);

        when(vggFactory.from(any(List.class))).thenReturn(mapVggFactory);
        when(detectionVGGUpdate.apply(eq(mapVggFactory), eq(detectionMock)))
                .thenReturn(newDetectionAfterVggUpdateMock);
        when(detectionRepositoryMock.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        subject.accept(eventMock);

        verify(eventMock).getDetectionId();
        verify(detectionRepositoryMock).findById(detectionId);
        verify(eventMock).getFilteredTiledPixelPolygons();
        verify(filteredTiledPixelPolygons).polygons();
        verify(polygonObjectTypeSerializableMock).polygonAsString();
        verify(geometryConverter)
                .readGeometryFromString(polygonObjectTypeSerializableMock.polygonAsString());
        verify(polygonObjectTypeSerializableMock).detectableType();
        verify(filteredTiledPixelPolygons).point();
        verify(filteredTiledPixelPolygons).tileX();
        verify(filteredTiledPixelPolygons).tileY();
        verify(filteredTiledPixelPolygons).zoom();
        verify(vggFactory).from(any(List.class));
        verify(detectionVGGUpdate).apply(eq(mapVggFactory), eq(detectionMock));
        verify(detectionRepositoryMock).save(eq(newDetectionAfterVggUpdateMock));
    }

}
