package app.bpartners.geojobs.model;

import java.time.Instant;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionConsumptionLog {
  private String id;
  private String userId;
  private Long usageMetric;
  private SubscriptionConsumptionType consumptionType;
  private SubscriptionConsumptionUnit consumptionUnit;
  private String comment;
  private Instant creationDatetime;
}
