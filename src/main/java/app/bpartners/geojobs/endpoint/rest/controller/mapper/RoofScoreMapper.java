package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson.RoofScoreCategoryMapper;
import app.bpartners.geojobs.endpoint.rest.model.RoofScore;
import app.bpartners.geojobs.model.geometry.area.Rate;
import app.bpartners.geojobs.model.geometry.area.RoofCondition;
import app.bpartners.geojobs.model.geometry.area.RoofScoreComputer;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofScoreMapper {
  private final RoofScoreCategoryMapper categoryMapper;
  private final RoofScoreComputer computer;

  public RoofScore from(RoofCondition roofCondition) {
    double score = computer.getGlobalRate(roofCondition);
    Rate category = computer.getRate(score);

    return new RoofScore()
        .score(BigDecimal.valueOf(score))
        .category(categoryMapper.toRest(category));
  }
}
