package app.bpartners.geojobs.file;

import app.bpartners.geojobs.model.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageValidator implements Consumer<BufferedImage> {
  private final WhiteImageDetector whiteImageDetector;

  @Override
  public void accept(BufferedImage img) {
    if (whiteImageDetector.apply(img)) {
      throw new BadRequestException("Invalid white image detected");
    }
  }
}
