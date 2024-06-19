package app.bpartners.geojobs.file;

import app.bpartners.geojobs.PojaGenerated;

@PojaGenerated
@SuppressWarnings("all")
public record FileHash(FileHashAlgorithm algorithm, String value) {}
