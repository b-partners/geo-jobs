package app.bpartners.geojobs.service.area.mutation.model;

import java.io.File;

/** Information needed to detect a mutation between two ortho imagery dates of a parcel. */
public record MutationContext(InstantTile older, InstantTile mostRecent, File maskImageFile) {}
