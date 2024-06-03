package app.bpartners.geojobs.sys;

import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;
import static java.util.concurrent.Executors.newScheduledThreadPool;

import com.sun.management.UnixOperatingSystemMXBean;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit.SECONDS;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenFilesChecker {
  private final UnixOperatingSystemMXBean os;
  private final CommandLauncher commandLauncher = new CommandLauncher();

  private final ScheduledExecutorService scheduler = newScheduledThreadPool(1);
  private final Duration frequencyCheck = Duration.ofSeconds(10);

  public OpenFilesChecker() {
    this.os =
        getOperatingSystemMXBean() instanceof UnixOperatingSystemMXBean
            ? (UnixOperatingSystemMXBean) getOperatingSystemMXBean()
            : null;
    if (os == null) {
      throw new RuntimeException("Cannot check open files on non-unix systems");
    }
  }

  public void start() {
    os.getOpenFileDescriptorCount();
    log.info("Checking open files every: {}", frequencyCheck);
    scheduler.scheduleAtFixedRate(this::checkOpenFiles, 0, frequencyCheck.toSeconds(), SECONDS);
  }

  private void checkOpenFiles() {
    var lsofByJava = commandLauncher.apply("lsof | grep java");
    var lsofByJavaCount = commandLauncher.apply("lsof | grep java | wc -l");
    var lsofAllCount = commandLauncher.apply("lsof | wc -l");
    var ofCountByMxBean = os.getOpenFileDescriptorCount();
    var maxOfByMxBean = os.getMaxFileDescriptorCount();
    log.info(
        "lsofByJava={}, lsofByJavaCount={}, lsofAllCount={}, ofCountByMxBean={}, maxOfByMxBean={}",
        lsofByJava,
        lsofByJavaCount,
        lsofAllCount,
        ofCountByMxBean,
        maxOfByMxBean);
  }

  public void stop() {
    scheduler.shutdown();
  }
}
