package app.bpartners.geojobs.endpoint.event.consumer.model;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
