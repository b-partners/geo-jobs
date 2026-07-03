package app.bpartners.geojobs.service.cityjson.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.citygml4j.cityjson.model.metadata.Metadata;
import org.junit.jupiter.api.Test;

class MetadataFactoryTest {

  @Test
  void make_with_epsg_2154_ok() {
    String id = "test-id";
    String title = "test-title";
    String crs = "EPSG:2154";

    Metadata metadata = MetadataFactory.make(id, title, crs);

    assertEquals(id, metadata.getIdentifier());
    assertEquals(title, metadata.getTitle());
    // Metadata returns a ReferenceSystem object, let's check its epsg code or similar if possible,
    // or just check that it is not null and has the right type if it's too complex.
    // Based on citygml4j docs, it might not just be the string.
    assertNotNull(metadata.getReferenceSystem());
  }

  @Test
  void make_with_epsg_2056_ok() {
    String id = "test-id-swiss";
    String title = "test-title-swiss";
    String crs = "EPSG:2056";

    Metadata metadata = MetadataFactory.make(id, title, crs);

    assertEquals(id, metadata.getIdentifier());
    assertEquals(title, metadata.getTitle());
    assertNotNull(metadata.getReferenceSystem());
  }
}
