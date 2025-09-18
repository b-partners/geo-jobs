package app.bpartners.geojobs.repository.model.detection;

public enum RoofCoveringType {
  ROOF_ARDOISE,
  ROOF_ASPHALTE_BITUME,
  ROOF_BAC_ACIER,
  ROOF_BETON_BRUT,
  ROOF_FIBRO_CIMENT,
  ROOF_GRAVIER,
  ROOF_MEMBRANE_SYNTHETIQUE,
  ROOF_TOLE_ONDULEE,
  ROOF_TUILES,
  ROOF_ZINC;

  public static RoofCoveringType of(String stringValue) {
    if (stringValue == null) return null;
    for (RoofCoveringType type : values()) {
      if (type.name().toLowerCase().equals(stringValue)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown RoofCoveringType: " + stringValue);
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
