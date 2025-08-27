package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_LABEL_PROPERTY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectionResponseAggregator
    implements BiFunction<
        List<DetectionResponseAggregator.DetectionResponseUrl>, String, DetectionResponse> {

  @Override
  public DetectionResponse apply(List<DetectionResponseUrl> responseUrls, String defaultUrl) {
    if (responseUrls == null || responseUrls.isEmpty()) {
      return null;
    }

    Map<String, DetectionResponse.ImageData> aggregatedRstRaw = new HashMap<>();
    Map<String, DetectionResponse.ImageData.Region> labelToRegion = new HashMap<>();
    Map<String, String> labelToUrl = new HashMap<>();

    int regionCounter = 0;

    for (var r : responseUrls) {
      DetectionResponse response = r.detectionResponse();
      String actualUrl = r.apiUrl();
      log.info("debug response rstRaw {}; url {}", response.getRstRaw().toString(), actualUrl);

      if (response.getRstRaw() == null) continue;

      for (Map.Entry<String, DetectionResponse.ImageData> entry : response.getRstRaw().entrySet()) {
        String imgKey = entry.getKey();
        DetectionResponse.ImageData imgData = entry.getValue();

        aggregatedRstRaw.computeIfAbsent(
            imgKey,
            k ->
                DetectionResponse.ImageData.builder()
                    .fileref(imgData.getFileref())
                    .size(imgData.getSize())
                    .filename(imgData.getFilename())
                    .base64ImgData(imgData.getBase64ImgData())
                    .fileAttributes(
                        imgData.getFileAttributes() != null
                            ? new HashMap<>(imgData.getFileAttributes())
                            : new HashMap<>())
                    .regions(new HashMap<>())
                    .build());

        if (imgData.getRegions() != null) {
          for (Map.Entry<String, DetectionResponse.ImageData.Region> regionEntry :
              imgData.getRegions().entrySet()) {
            DetectionResponse.ImageData.Region region = regionEntry.getValue();

            String label = region.getRegionAttributes().get(REGION_LABEL_PROPERTY);

            if (label != null) {
              var existingUrl = labelToUrl.get(label);
              if (existingUrl != null) {
                if (!defaultUrl.equals(existingUrl)) {
                  if (defaultUrl.equals(actualUrl)) {
                    continue;
                  }
                } else if (!defaultUrl.equals(actualUrl)) {
                  labelToRegion.put(label, region);
                  labelToUrl.put(label, actualUrl);
                  continue;
                } else {
                  // Both existingUrl and actualUrl are from the default API
                  continue;
                }
              }
              labelToRegion.put(label, region);
              labelToUrl.put(label, actualUrl);
            }
          }
        }
      }
    }

    for (DetectionResponse.ImageData imgData : aggregatedRstRaw.values()) {
      Map<String, DetectionResponse.ImageData.Region> finalRegions = new HashMap<>();
      for (Map.Entry<String, DetectionResponse.ImageData.Region> entry : labelToRegion.entrySet()) {
        finalRegions.put("region_" + (++regionCounter), entry.getValue());
      }
      imgData.setRegions(finalRegions);
    }

    DetectionResponse aggregated = new DetectionResponse();
    aggregated.setSrcImageUrl(responseUrls.getFirst().detectionResponse().getSrcImageUrl());
    aggregated.setRstImageUrl(responseUrls.getFirst().detectionResponse().getRstImageUrl());
    aggregated.setRstRaw(aggregatedRstRaw);
    log.info("aggregated rstRaw {}", aggregatedRstRaw);

    return aggregated;
  }

  public record DetectionResponseUrl(DetectionResponse detectionResponse, String apiUrl) {}
}
