package app.bpartners.geojobs.service.area.mutation;

import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MutationComputer {
  // The mutation-degat-api's ONNX model requires fixed 256x256 inputs.
  private static final int MUTATION_MODEL_INPUT_SIZE = 256;

  private final MutationApi api;
  private final ImageDownloader imageDownloader;
  private final ImageResizer imageResizer;

  public MutationType apply(MutationContext context) {
    var oldImageFile = imageDownloader.download(context.older().imagePresignedUrl().value());
    var recentImageFile =
        imageDownloader.download(context.mostRecent().imagePresignedUrl().value());

    var resizedOld = imageResizer.resizePhoto(oldImageFile, MUTATION_MODEL_INPUT_SIZE);
    var resizedRecent = imageResizer.resizePhoto(recentImageFile, MUTATION_MODEL_INPUT_SIZE);
    var resizedMask = imageResizer.resizeMask(context.maskImageFile(), MUTATION_MODEL_INPUT_SIZE);

    var filename = "mutation_" + UUID.randomUUID();
    var mutationResponse = api.detectMutation(resizedOld, resizedRecent, resizedMask, filename);

    return mutationResponse.mutation();
  }
}
