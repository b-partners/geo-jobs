package app.bpartners.geojobs;

import static app.bpartners.geojobs.concurrency.ThreadRenamer.renameWorkerThread;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;

import app.bpartners.geojobs.endpoint.event.consumer.EventConsumer;
import app.bpartners.geojobs.endpoint.event.consumer.model.ConsumableEvent;
import app.bpartners.geojobs.endpoint.event.consumer.model.ConsumableEventTyper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@PojaGenerated
@SuppressWarnings("all")
public class MailboxEventHandler implements RequestHandler<SQSEvent, String> {

  public static final String SPRING_SERVER_PORT_FOR_RANDOM_VALUE = "0";

  @Override
  public String handleRequest(SQSEvent event, Context context) {
    renameWorkerThread(currentThread());
    log.info("Received: event={}, awsReqId={}", event, context.getAwsRequestId());
    List<SQSMessage> messages = event.getRecords();
    log.info("SQS messages: {}", messages);

    var applicationContext = applicationContext();
    closeResourcesOnShutdown(applicationContext);
    consume(applicationContext, messages);
    closeResources(applicationContext);

    return "ok";
  }

  private static void consume(
      ConfigurableApplicationContext applicationContext, List<SQSMessage> messages) {
    var eventConsumer = applicationContext.getBean(EventConsumer.class);
    var consumableEventTyper = applicationContext.getBean(ConsumableEventTyper.class);
    var consumableEvents = consumableEventTyper.apply(messages);
    consumableEvents.forEach(ConsumableEvent::newRandomVisibilityTimeout); // note(init-visibility)
    eventConsumer.accept(consumableEventTyper.apply(messages));
  }

  private void closeResourcesOnShutdown(ConfigurableApplicationContext applicationContext) {
    getRuntime()
        .addShutdownHook(
            // in case, say, the execution timed out
            // TODO: not enough. Indeed, we have no control over when AWS shuts the JVM down
            //   Best is to regularly check whether we are nearing end of allowedTime,
            //   in which case we close resources before timing out.
            //   Frontal functions might have the same issue also.
            new Thread(() -> closeResources(applicationContext)));
  }

  private void closeResources(ConfigurableApplicationContext applicationContext) {
    try {
      var hikariDatasource = applicationContext.getBean(HikariDataSource.class);
      hikariDatasource.close();

      applicationContext.close();
    } catch (Exception ignored) {
    }
  }

  private ConfigurableApplicationContext applicationContext(String... args) {
    SpringApplication application = new SpringApplication(PojaApplication.class);
    application.setDefaultProperties(
        Map.of(
            "spring.flyway.enabled", "false", "server.port", SPRING_SERVER_PORT_FOR_RANDOM_VALUE));
    application.setAdditionalProfiles("worker");
    return application.run(args);
  }
}
