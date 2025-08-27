package app.bpartners.geojobs.service.tile19;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ExtenderApi implements Function<List<File>, String> {
  private final RestTemplate restTemplate = new RestTemplate();
  private String TILE_EXTENDER_API_URL = System.getenv("TILE_EXTENDER_API_URL");

  @Override
  public String apply(List<File> imageFiles) {
    if (imageFiles == null) {
      throw new IllegalArgumentException("imageFiles cannot be null");
    }
    if (imageFiles.size() != 9) {
      throw new IllegalArgumentException(
          "imageFiles must contain 9 images but actually contain " + imageFiles.size());
    }
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

    for (int i = 0; i < imageFiles.size(); i++) {
      String paramName = "file" + (i + 1);
      body.add(paramName, new FileSystemResource(imageFiles.get(i)));
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    var url = TILE_EXTENDER_API_URL + "/extend/existing-tiles";
    return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class).getBody();
  }
}
