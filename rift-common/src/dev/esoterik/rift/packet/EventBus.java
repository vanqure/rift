package dev.esoterik.rift.packet;

import dev.esoterik.rift.codec.Packet;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public final class EventBus {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  private final BiConsumer<Packet, Packet> packetProcessor;
  private final Map<String, Map<Class<? extends Packet>, Set<Subscription>>>
      subscriptionsByEventTypeAndTopic = new ConcurrentHashMap<>();

  public EventBus(final BiConsumer<Packet, Packet> packetProcessor) {
    this.packetProcessor = packetProcessor;
  }

  public Set<Class<? extends Packet>> subscribe(final PacketSubscriber packetSubscriber) {
    final Map<Class<? extends Packet>, Set<MethodHandle>> methodReferencesByEventType =
        getSubscriptionsByType(packetSubscriber.getClass());

    for (final Map.Entry<Class<? extends Packet>, Set<MethodHandle>> entry :
        methodReferencesByEventType.entrySet()) {
      final Class<? extends Packet> eventType = entry.getKey();
      final Set<MethodHandle> invocations = entry.getValue();
      final String topic = packetSubscriber.topic();

      subscriptionsByEventTypeAndTopic
          .computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
          .computeIfAbsent(eventType, k -> new HashSet<>())
          .add(new Subscription(packetSubscriber, invocations));
    }

    return methodReferencesByEventType.keySet();
  }

  public void publish(final Packet packet, final String topic) {
    final Map<Class<? extends Packet>, Set<Subscription>> subscriptionsByEventType =
        subscriptionsByEventTypeAndTopic.get(topic);
    if (subscriptionsByEventType == null) {
      return;
    }

    final Class<? extends Packet> eventType = packet.getClass();
    final Set<Subscription> subscriptions =
        subscriptionsByEventType.getOrDefault(eventType, Collections.emptySet());
    if (subscriptions.isEmpty()) {
      return;
    }

    notifySubscription(packet, subscriptions);
  }

  public void publish(final Packet packet) {
    publish(packet, "");
  }

  private void notifySubscription(final Packet packet, final Set<Subscription> subscriptions) {
    for (final Subscription subscription : subscriptions) {
      for (final MethodHandle invocation : subscription.invocations()) {
        final PacketSubscriber packetSubscriber = subscription.packetSubscriber();
        notifySubscribedMethods(packetSubscriber, invocation, packet);
      }
    }
  }

  private void notifySubscribedMethods(
      final PacketSubscriber packetSubscriber,
      final MethodHandle invocation,
      final Packet packet) {
    try {
      final Object returnedValue = invocation.invoke(packetSubscriber, packet);
      if (returnedValue != null) {
        tryProcessing(packet, returnedValue);
      }
    } catch (final Throwable exception) {
      throw new IllegalStateException(
          "Could not publish packet, because of unexpected exception during method invocation.",
          exception);
    }
  }

  private Map<Class<? extends Packet>, Set<MethodHandle>> getSubscriptionsByType(
      final Class<? extends PacketSubscriber> subscriberType) {
    return Arrays.stream(subscriberType.getDeclaredMethods())
        .filter(this::isSubscribedMethod)
        .map(method -> getMethodHandle(subscriberType, method))
        .collect(Collectors.groupingBy(this::getSubscribedEvent, Collectors.toSet()));
  }

  private boolean isSubscribedMethod(final Method method) {
    return method.isAnnotationPresent(PacketHandler.class)
        && method.getParameterCount() == 1
        && Packet.class.isAssignableFrom(method.getParameterTypes()[0]);
  }

  private MethodHandle getMethodHandle(final Class<?> subscriberType, final Method method) {
    try {
      return getLookupForClass(subscriberType).unreflect(method);
    } catch (final Exception exception) {
      throw new IllegalStateException(
          "Could not resolve method handle for %s method, because of illegal access."
              .formatted(method.getName()),
          exception);
    }
  }

  @SuppressWarnings("unchecked")
  private Class<? extends Packet> getSubscribedEvent(final MethodHandle method) {
    return (Class<? extends Packet>) method.type().lastParameterType();
  }

  private MethodHandles.Lookup getLookupForClass(final Class<?> clazz)
      throws IllegalAccessException {
    return Modifier.isPublic(clazz.getModifiers())
        ? LOOKUP
        : MethodHandles.privateLookupIn(clazz, LOOKUP);
  }

  private void tryProcessing(final Packet packet, @Nullable final Object value) {
    if (value == null) {
      return;
    }

    final Class<?> resultType = value.getClass();
    if (resultType == CompletableFuture.class) {
      processPromise(packet, (CompletionStage<?>) value);
      return;
    }

    if (!Packet.class.isAssignableFrom(resultType)) {
      return;
    }

    packetProcessor.accept(packet, (Packet) value);
  }

  private <P extends Packet, T> void processPromise(
      final P packet, final CompletionStage<T> resultFuture) {
    resultFuture.whenComplete(
        (result, throwable) -> {
          if (throwable != null) {
            throw new IllegalStateException(
                "Could not handle result of type %s, because of an exception."
                    .formatted(throwable.getClass().getName()),
                throwable);
          }

          tryProcessing(packet, result);
        });
  }

  private record Subscription(
      PacketSubscriber packetSubscriber, Set<MethodHandle> invocations) {

    @Override
    public boolean equals(final Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final Subscription that = (Subscription) o;
      return Objects.equals(packetSubscriber.topic(), that.packetSubscriber.topic())
          && Objects.equals(packetSubscriber.getClass(), that.packetSubscriber.getClass());
    }

    @Override
    public int hashCode() {
      return Objects.hash(packetSubscriber.topic(), packetSubscriber.getClass());
    }
  }
}
