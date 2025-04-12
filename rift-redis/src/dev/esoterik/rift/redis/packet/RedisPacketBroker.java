package dev.esoterik.rift.redis.packet;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import dev.esoterik.rift.codec.Packet;
import dev.esoterik.rift.codec.Serializer;
import dev.esoterik.rift.packet.EventBus;
import dev.esoterik.rift.packet.PacketBroker;
import dev.esoterik.rift.packet.PacketSubscriber;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RedisPacketBroker<P extends Packet> implements PacketBroker<P> {

  private final String identity;
  private final EventBus eventBus;
  private final Serializer serializer;
  private final StatefulRedisConnection<String, String> connection;
  private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
  private final Map<String, CompletableFuture<?>> callbacks = new ConcurrentHashMap<>();
  private final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();

  private RedisPacketBroker(
      final String identity,
      final Serializer serializer,
      final StatefulRedisConnection<String, String> connection,
      final StatefulRedisPubSubConnection<String, String> pubSubConnection) {
    this.identity = identity;
    this.eventBus =
        new EventBus(
            (request, response) -> {
              //noinspection unchecked
              delegateCallback((P) request, (P) response);
            });
    this.serializer = serializer;
    this.connection = connection;
    this.pubSubConnection = pubSubConnection;
    subscribeCallbacks();
  }

  public static <P extends Packet> PacketBroker<P> create(
      final String identity,
      final Serializer serializer,
      final StatefulRedisConnection<String, String> connection,
      final StatefulRedisPubSubConnection<String, String> pubSubConnection) {
    return new RedisPacketBroker<>(identity, serializer, connection, pubSubConnection);
  }

  @Override
  public String getIdentity() {
    return identity;
  }

  private void delegateCallback(final P request, final P response) {
    requireNonNull(
        request.source(), "Could not delegate packet to packet broker due to missing source.");
    publish("callbacks", response);
  }

  private void subscribeCallbacks() {
    pubSubConnection.addListener(
        new RedisPacketDelegate(
            "callbacks",
            message -> {
              final P response = serializer.deserialize(message);
              if (response.target() == null) {
                return;
              }

              final CompletableFuture<?> future = callbacks.get(response.target());
              if (future == null) {
                return;
              }

              //noinspection unchecked
              ((CompletableFuture<P>) future).complete(response);
            }));
    pubSubConnection.sync().subscribe("callbacks");
  }

  @Override
  public void subscribe(final PacketSubscriber packetSubscriber) {
    eventBus.subscribe(packetSubscriber);

    final String topic = packetSubscriber.topic();
    subscribePacketBroker(
        topic,
        message -> {
          final P packet = serializer.deserialize(message);
          eventBus.publish(packet, topic);
        });
  }

  @Override
  public void publish(final String channelName, final P packet) {
    try {
      if (packet.source() == null) {
        packet.source(identity);
      }
      connection.sync().publish(channelName, serializer.serialize(packet));
    } catch (final Exception exception) {
      throw new IllegalStateException(
          "Could not publish packet on channel named %s due to unexpected exception."
              .formatted(channelName),
          exception);
    }
  }

  @Override
  public <R extends P> CompletableFuture<R> request(final String channelName, final P request) {
    request.source(identity + randomUUID());

    final CompletableFuture<R> future = new CompletableFuture<>();
    callbacks.put(request.source(), future);

    publish(channelName, request);
    return future;
  }

  private void subscribePacketBroker(final String channelName, final Consumer<String> callback) {
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
