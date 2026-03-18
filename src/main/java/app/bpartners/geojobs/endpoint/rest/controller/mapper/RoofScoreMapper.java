package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson.RoofScoreCategoryMapper;
import app.bpartners.geojobs.endpoint.rest.model.RoofScore;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoofScoreMapper {
  private final RoofScoreCategoryMapper categoryMapper;

  public RoofScore toRest(app.bpartners.geojobs.model.geometry.area.RoofScore domain) {
    return new RoofScore()
        .score(BigDecimal.valueOf(domain.score()))
        .category(categoryMapper.toRest(domain.category()));
  }
}
