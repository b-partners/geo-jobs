package app.bpartners.geojobs.endpoint.rest.postprocessing.mapper;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.function.Function;

@AllArgsConstructor
@Component
public class FileMapper implements Function<File,Geojson> {

    @Override
    public Geojson apply(File geojsonFile) {
        return new Geojson(geojsonFile);
    }
}

