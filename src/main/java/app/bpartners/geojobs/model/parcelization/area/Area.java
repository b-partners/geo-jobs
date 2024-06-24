package app.bpartners.geojobs.model.parcelization.area;

public abstract sealed class Area implements Comparable<Area> permits MetricArea, SquareDegree {}
