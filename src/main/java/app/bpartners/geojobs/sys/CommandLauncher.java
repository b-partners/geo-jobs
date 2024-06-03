package app.bpartners.geojobs.sys;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.util.Scanner;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandLauncher implements Function<String, String> {
  @Override
  public String apply(String cmd) {
    try {
      Process process = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});
      if (!process.waitFor(1, MINUTES)) {
        log.error("cmd={} took more than  1 minute to complete", cmd);
        process.destroyForcibly();
      }
      Scanner inputScanner = new Scanner(process.getInputStream()).useDelimiter("\\A");
      String inputMessage = inputScanner.hasNext() ? inputScanner.next() : "";

      Scanner errorScanner = new Scanner(process.getErrorStream()).useDelimiter("\\A");
      String errorMessage = errorScanner.hasNext() ? ("\nError(s):\n" + errorScanner.next()) : "";

      return inputMessage + errorMessage;
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
