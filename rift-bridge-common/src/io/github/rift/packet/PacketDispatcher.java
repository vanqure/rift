package io.github.rift.packet;

import io.github.rift.codec.Packet;
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

public final class PacketDispatcher {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final BiConsumer<Packet, Packet> packetConsumer;
    private final Map<String, Map<Class<? extends Packet>, Set<Subscription>>> subscriptionsByPacketTypeAndTopic =
            new ConcurrentHashMap<>();

    public PacketDispatcher(BiConsumer<Packet, Packet> packetConsumer) {
        this.packetConsumer = packetConsumer;
    }

    public Set<Class<? extends Packet>> subscribe(PacketSubscriber packetSubscriber) {
        Map<Class<? extends Packet>, Set<MethodHandle>> methodReferencesByPacketType =
                getSubscriptionsByType(packetSubscriber.getClass());

        for (Map.Entry<Class<? extends Packet>, Set<MethodHandle>> entry : methodReferencesByPacketType.entrySet()) {
            Class<? extends Packet> packetType = entry.getKey();
            Set<MethodHandle> invocations = entry.getValue();
            String topic = packetSubscriber.topic();

            subscriptionsByPacketTypeAndTopic
                    .computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(packetType, k -> new HashSet<>())
                    .add(new Subscription(packetSubscriber, invocations));
        }

        return methodReferencesByPacketType.keySet();
    }

    public void dispatch(Packet packet, String topic) {
        Map<Class<? extends Packet>, Set<Subscription>> subscriptionsByPacketType =
                subscriptionsByPacketTypeAndTopic.get(topic);
        if (subscriptionsByPacketType == null) {
            return;
        }

        Class<? extends Packet> packetType = packet.getClass();
        Set<Subscription> subscriptions = subscriptionsByPacketType.getOrDefault(packetType, Collections.emptySet());
        if (subscriptions.isEmpty()) {
            return;
        }

        notifySubscription(packet, subscriptions);
    }

    private void notifySubscription(Packet packet, Set<Subscription> subscriptions) {
        for (Subscription subscription : subscriptions) {
            for (MethodHandle invocation : subscription.invocations()) {
                PacketSubscriber packetSubscriber = subscription.packetSubscriber();
                notifySubscribedMethods(packetSubscriber, invocation, packet);
            }
        }
    }

    private void notifySubscribedMethods(PacketSubscriber packetSubscriber, MethodHandle invocation, Packet packet) {
        try {
            Object returnedValue = invocation.invoke(packetSubscriber, packet);
            if (returnedValue != null) {
                tryProcessing(packet, returnedValue);
            }
        } catch (Throwable exception) {
            throw new IllegalStateException(
                    "Could not publish packet, because of unexpected exception during method invocation.", exception);
        }
    }

    private Map<Class<? extends Packet>, Set<MethodHandle>> getSubscriptionsByType(
            Class<? extends PacketSubscriber> subscriberType) {
        return Arrays.stream(subscriberType.getDeclaredMethods())
                .filter(this::isSubscribedMethod)
                .map(method -> getMethodHandle(subscriberType, method))
                .collect(Collectors.groupingBy(this::getSubscribedPacket, Collectors.toSet()));
    }

    private boolean isSubscribedMethod(Method method) {
        return method.isAnnotationPresent(PacketSubscribe.class)
                && method.getParameterCount() == 1
                && Packet.class.isAssignableFrom(method.getParameterTypes()[0]);
    }

    private MethodHandle getMethodHandle(Class<?> subscriberType, Method method) {
        try {
            return getLookupForClass(subscriberType).unreflect(method);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not resolve method handle for %s method, because of illegal access."
                            .formatted(method.getName()),
                    exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Packet> getSubscribedPacket(MethodHandle method) {
        return (Class<? extends Packet>) method.type().lastParameterType();
    }

    private MethodHandles.Lookup getLookupForClass(Class<?> clazz) throws IllegalAccessException {
        return Modifier.isPublic(clazz.getModifiers()) ? LOOKUP : MethodHandles.privateLookupIn(clazz, LOOKUP);
    }

    private void tryProcessing(Packet packet, @Nullable Object value) {
        if (value == null) {
            return;
        }

        Class<?> resultType = value.getClass();
        if (resultType == CompletableFuture.class) {
            processPromise(packet, (CompletionStage<?>) value);
            return;
        }

        if (!Packet.class.isAssignableFrom(resultType)) {
            return;
        }

        packetConsumer.accept(packet, (Packet) value);
    }

    private <P extends Packet, T> void processPromise(P packet, CompletionStage<T> resultFuture) {
        resultFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                throw new IllegalStateException(
                        "Could not handle result of type %s, because of an exception."
                                .formatted(throwable.getClass().getName()),
                        throwable);
            }

            tryProcessing(packet, result);
        });
    }

    private record Subscription(PacketSubscriber packetSubscriber, Set<MethodHandle> invocations) {

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Subscription that = (Subscription) o;
            return Objects.equals(packetSubscriber.topic(), that.packetSubscriber.topic())
                    && Objects.equals(packetSubscriber.getClass(), that.packetSubscriber.getClass());
        }

        @Override
        public int hashCode() {
            return Objects.hash(packetSubscriber.topic(), packetSubscriber.getClass());
        }
    }
}
