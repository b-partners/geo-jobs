package app.bpartners.geojobs.model.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleCaptchaResponse {
  private boolean success;
  private Double score;
}
