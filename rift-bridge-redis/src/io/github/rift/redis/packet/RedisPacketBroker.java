package io.github.rift.redis.packet;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import io.github.rift.codec.Packet;
import io.github.rift.codec.Serializer;
import io.github.rift.packet.PacketBroker;
import io.github.rift.packet.PacketDispatcher;
import io.github.rift.packet.PacketSubscriber;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RedisPacketBroker<P extends Packet> implements PacketBroker<P> {

    private final String identity;
    private final PacketDispatcher dispatcher;
    private final Serializer serializer;
    private final StatefulRedisConnection<String, String> connection;
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final Map<String, CompletableFuture<?>> callbacks = new ConcurrentHashMap<>();
    private final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();

    private RedisPacketBroker(
            String identity,
            Serializer serializer,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection) {
        this.identity = identity;
        this.dispatcher = new PacketDispatcher((request, response) -> {
            //noinspection unchecked
            delegateCallback((P) request, (P) response);
        });
        this.serializer = serializer;
        this.connection = connection;
        this.pubSubConnection = pubSubConnection;
        subscribeCallbacks();
    }

    public static <P extends Packet> PacketBroker<P> create(
            String identity,
            Serializer serializer,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection) {
        return new RedisPacketBroker<>(identity, serializer, connection, pubSubConnection);
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    private void delegateCallback(P request, P response) {
        requireNonNull(
                request.source(), "Could not delegate packet to packet broker due to missing source.");
        publish("callbacks", response);
    }

    private void subscribeCallbacks() {
        pubSubConnection.addListener(new RedisPacketDelegate(
                "callbacks", message -> {
            P response = serializer.deserialize(message);
            if (response.target() == null) {
                return;
            }

            CompletableFuture<?> future = callbacks.get(response.target());
            if (future == null) {
                return;
            }

            //noinspection unchecked
            ((CompletableFuture<P>) future).complete(response);
        }));
        pubSubConnection.sync().subscribe("callbacks");
    }

    @Override
    public void subscribe(PacketSubscriber packetSubscriber) {
        dispatcher.subscribe(packetSubscriber);

        String topic = packetSubscriber.topic();
        subscribePacketBroker(
                topic, message -> {
                    P packet = serializer.deserialize(message);
                    dispatcher.dispatch(packet, topic);
                });
    }

    @Override
    public void publish(String channelName, P packet) {
        try {
            if (packet.source() == null) {
                packet.source(identity);
            }
            connection.sync().publish(channelName, serializer.serialize(packet));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not publish packet on channel named %s due to unexpected exception."
                            .formatted(channelName),
                    exception);
        }
    }

    @Override
    public <R extends P> CompletableFuture<R> request(String channelName, P request) {
        request.source(identity + randomUUID());

        CompletableFuture<R> future = new CompletableFuture<>();
        callbacks.put(request.source(), future);

        publish(channelName, request);
        return future;
    }

    private void subscribePacketBroker(String channelName, Consumer<String> callback) {
        if (subscribedTopics.contains(channelName)) {
            return;
        }

        subscribedTopics.add(channelName);

        pubSubConnection.addListener(new RedisPacketDelegate(channelName, callback));
        pubSubConnection.sync().subscribe(channelName);
    }

    @Override
    public void close() {
        subscribedTopics.clear();
    }
}
