package app.bpartners.geojobs.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ZoneSimilarityConf {
  private final boolean enabled;
  private final double minIou;
  private final double maxDisplacementInMeters;

  @Autowired
  public ZoneSimilarityConf(
      @Value("${detection.reuse.approximate.enabled:true}") boolean enabled,
      @Value("${detection.reuse.approximate.min-iou:0.95}") double minIou,
      @Value("${detection.reuse.approximate.max-displacement-in-meters:1.0}")
          double maxDisplacementInMeters) {
    this.enabled = enabled;
    this.minIou = minIou;
    this.maxDisplacementInMeters = maxDisplacementInMeters;
  }
}
