package app.bpartners.geojobs.endpoint.event.consumer.model;

import app.bpartners.geojobs.PojaGenerated;

@PojaGenerated
public record TypedEvent(String typeName, Object payload) {}
