package app.bpartners.geojobs.endpoint.event.consumer;

import static org.reflections.scanners.Scanners.SubTypes;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.endpoint.event.consumer.model.TypedEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@PojaGenerated
@SuppressWarnings("all")
@AllArgsConstructor
@Component
@Slf4j
public class EventServiceInvoker implements Consumer<TypedEvent> {

  private final ApplicationContext applicationContext;

  @Override
  public void accept(TypedEvent typedEvent) {
    var typeName = typedEvent.typeName();
    var eventClasses = getAllClasses("app.bpartners.geojobs.endpoint.event.model");
    for (var clazz : eventClasses) {
      if (clazz.getTypeName().equals(typeName)) {
        try {
          faillibleInvoke(clazz, typeName, typedEvent);
          return;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    throw new RuntimeException("Unexpected type for event=" + typedEvent);
  }

  private void faillibleInvoke(Class<?> clazz, String typeName, TypedEvent typedEvent)
      throws ClassNotFoundException,
          NoSuchMethodException,
          IllegalAccessException,
          InvocationTargetException {
    var serviceClazz = Class.forName(getEventService(typeName));
    var acceptMethod =
        // TODO: Does not work when serviceClazz gets its accept method from a super-class
        serviceClazz.getMethod("accept", clazz);
    log.info("Invoke: class={}, method={}", serviceClazz, acceptMethod);
    acceptMethod.invoke(applicationContext.getBean(serviceClazz), typedEvent.payload());
  }

  private String getEventService(String eventClazzName) {
    var typeNameAsArray = eventClazzName.split("\\.");
    return "app.bpartners.geojobs.service.event."
        + typeNameAsArray[typeNameAsArray.length - 1]
        + "Service";
  }

  private Set<Class<?>> getAllClasses(String packageName) {
    var reflections = new Reflections(packageName, SubTypes.filterResultsBy(s -> true));
    return new HashSet<>(reflections.getSubTypesOf(Object.class));
  }
}
