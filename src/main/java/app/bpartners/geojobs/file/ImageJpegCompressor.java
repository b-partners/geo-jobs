package app.bpartners.geojobs.file;

import static java.util.UUID.randomUUID;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.BiFunction;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class ImageJpegCompressor implements BiFunction<File, Float, File> {

  public static final String JPG_SUFFIX = "jpg";

  @SneakyThrows
  @Override
  public File apply(File inputFile, Float quality) {
    String randomName = randomUUID().toString();
    File compressedTmpFile = File.createTempFile(randomName, JPG_SUFFIX);
    BufferedImage image = ImageIO.read(inputFile);

    ImageWriter writer = ImageIO.getImageWritersByFormatName(JPG_SUFFIX).next();
    ImageOutputStream ios = ImageIO.createImageOutputStream(compressedTmpFile);
    writer.setOutput(ios);

    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(quality);

    writer.write(null, new IIOImage(image, null, null), param);

    ios.close();
    writer.dispose();
    return compressedTmpFile;
  }
}
