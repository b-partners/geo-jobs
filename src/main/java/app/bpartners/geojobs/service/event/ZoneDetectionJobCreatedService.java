package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobCreated;
import app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;

import java.util.List;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneDetectionJobCreatedService implements Consumer<ZoneDetectionJobCreated> {
    private final ZoneDetectionJobService zoneDetectionJobService;
    private final EventProducer eventProducer;

    @Override
    public void accept(ZoneDetectionJobCreated zoneDetectionJobCreated) {
        var zoneDetectionJob = zoneDetectionJobCreated.getZoneDetectionJob();
        var detectionType = zoneDetectionJob.getType();
        switch (detectionType) {
            case MACHINE -> {
                ZoneDetectionJob humanZDJ = zoneDetectionJob.toBuilder()
                        .type(ZoneDetectionJob.DetectionType.HUMAN)
                        .build();
                eventProducer.accept(List.of(ZoneDetectionJobCreated.builder()
                        .zoneDetectionJob(humanZDJ)
                        .build()));
                zoneDetectionJobService.fireTasks(zoneDetectionJob.getId());
            }
            case HUMAN -> zoneDetectionJobService.save(zoneDetectionJob);
            default -> throw new RuntimeException("Unknown ZDJ detection type : " + detectionType);
        }
    }
}
