package app.bpartners.geojobs.concurrency;

import static app.bpartners.geojobs.concurrency.ThreadRenamer.getRandomSubThreadNamePrefixFrom;
import static app.bpartners.geojobs.concurrency.ThreadRenamer.renameThread;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newFixedThreadPool;

import app.bpartners.geojobs.PojaGenerated;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@PojaGenerated
@Component
public class Workers<T> {
  private final ExecutorService executorService;

  public Workers(@Value("${workers.thread.number:5}") int nbThreads) {
    this.executorService = newFixedThreadPool(nbThreads);
  }

  @SneakyThrows
  public List<Future<T>> invokeAll(List<Callable<T>> callables) {
    var parentThread = currentThread();
    callables =
        callables.stream()
            .map(
                c ->
                    (Callable<T>)
                        () -> {
                          renameThread(
                              currentThread(), getRandomSubThreadNamePrefixFrom(parentThread));
                          return c.call();
                        })
            .toList();
    return executorService.invokeAll(callables);
  }
}
