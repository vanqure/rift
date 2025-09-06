package io.github.rift.redis.packet;

import static java.util.UUID.randomUUID;

import io.github.rift.serializer.Packet;
import io.github.rift.serializer.Serializer;
import io.github.wisp.Wisp;
import io.github.wisp.subscription.Subscriber;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class RedisPacketBrokerImpl<P extends Packet> implements RedisPacketBroker<P> {

    private final Wisp wisp;
    private final String identity;
    private final Serializer serializer;
    private final Duration requestCleanupInterval;
    private final StatefulRedisConnection<String, String> connection;
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final Map<String, CompletableFuture<?>> callbacks = new ConcurrentHashMap<>();
    private final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();

    RedisPacketBrokerImpl(
            Wisp wisp,
            String identity,
            Serializer serializer,
            Duration requestCleanupInterval,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection) {
        this.wisp = wisp.result(
                Packet.class, (event, response) -> {
                    if (event instanceof Packet request) {
                        response.setReplyTo(request.getReplyTo());
                        //noinspection unchecked
                        publish("callbacks", (P) response);
                    }
                });
        this.identity = identity;
        this.serializer = serializer;
        this.requestCleanupInterval = requestCleanupInterval;
        this.connection = connection;
        this.pubSubConnection = pubSubConnection;
        subscribeCallbacks();
    }

    private void subscribeCallbacks() {
        pubSubConnection.addListener(new RedisPacketDelegate(
                "callbacks", message -> {
            P response = serializer.deserialize(message);
            if (response.getReplyTo() == null) {
                return;
            }

            CompletableFuture<?> future = callbacks.get(response.getReplyTo());
            if (future == null) {
                return;
            }

            //noinspection unchecked
            ((CompletableFuture<P>) future).complete(response);
        }));
        pubSubConnection.sync().subscribe("callbacks");
    }

    @Override
    public String identity() {
        return identity;
    }

    @Override
    public void publish(String topic, P packet) {
        try {
            connection.sync().publish(topic, serializer.serialize(packet));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not publish packet on channel named %s due to unexpected exception.".formatted(topic),
                    exception);
        }
    }

    @Override
    public <R extends P> CompletableFuture<R> request(String topic, P request) {
        String replyTo = randomUUID().toString();
        request.setReplyTo(replyTo);

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        responseFuture.orTimeout(requestCleanupInterval.toMillis(), TimeUnit.MILLISECONDS);

        subscribeInternal(replyTo, responseFuture::complete);

        responseFuture.whenCompleteAsync((response, throwable) -> pubSubConnection.sync().unsubscribe(replyTo));

        publish(topic, request);

        return responseFuture.thenApply(serializer::deserialize).thenApply(response -> {
            //noinspection unchecked
            return (R) response;
        });
    }

    @Override
    public void subscribe(Subscriber subscriber) {
        wisp.subscribe(subscriber);

        String topic = subscriber.topic();
        subscribeInternal(
                topic, message -> {
                    P packet = serializer.deserialize(message);
                    wisp.publish(packet, topic);
                });
    }

    private void subscribeInternal(String topic, Consumer<String> callback) {
        if (subscribedTopics.contains(topic)) {
            return;
        }

        subscribedTopics.add(topic);

        pubSubConnection.addListener(new RedisPacketDelegate(topic, callback));
        pubSubConnection.sync().subscribe(topic);
    }

    @Override
    public void close() {
        subscribedTopics.clear();
    }
}
