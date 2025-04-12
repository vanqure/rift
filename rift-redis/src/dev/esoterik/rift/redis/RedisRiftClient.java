package dev.esoterik.rift.redis;

import static java.lang.ProcessHandle.current;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import dev.esoterik.rift.RiftClient;
import dev.esoterik.rift.codec.Packet;
import dev.esoterik.rift.codec.Serializer;
import dev.esoterik.rift.lock.DistributedLock;
import dev.esoterik.rift.map.RiftMap;
import dev.esoterik.rift.packet.PacketBroker;
import dev.esoterik.rift.packet.PacketSubscriber;
import dev.esoterik.rift.redis.lock.RedisLock;
import dev.esoterik.rift.redis.map.RedisKeyValue;
import dev.esoterik.rift.redis.map.RedisMap;
import dev.esoterik.rift.redis.packet.RedisPacketBroker;
import dev.esoterik.rift.scheduler.Scheduler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public final class RedisRiftClient<S extends Serializable, P extends Packet>
    implements RiftClient<S, P> {

  private static final Duration DEFAULT_LOCK_ACQUIRE_DELAY = ofMillis(150L);
  private static final Duration DEFAULT_LOCK_ACQUIRE_UNTIL = ofSeconds(3L);

  private final String identity;

  private final Scheduler scheduler;
  private final Serializer serializer;

  private final PacketBroker<P> packetBroker;

  private final RedisKeyValue keyValue;

  private final StatefulRedisConnection<String, String> connection;
  private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

  private RedisRiftClient(
      final String identity,
      final Scheduler scheduler,
      final Serializer serializer,
      final PacketBroker<P> packetBroker,
      final RedisKeyValue keyValue,
      final StatefulRedisConnection<String, String> connection,
      final StatefulRedisPubSubConnection<String, String> pubSubConnection) {
    this.identity = identity;
    this.scheduler = scheduler;
    this.serializer = serializer;
    this.packetBroker = packetBroker;
    this.keyValue = keyValue;
    this.connection = connection;
    this.pubSubConnection = pubSubConnection;
  }

  public static <S extends Serializable, P extends Packet> RiftClient<S, P> create(
      final String identity,
      final Scheduler scheduler,
      final Serializer serializer,
      final PacketBroker<P> packetBroker,
      final RedisKeyValue keyValue,
      final StatefulRedisConnection<String, String> connection,
      final StatefulRedisPubSubConnection<String, String> pubSubConnection) {
    return new RedisRiftClient<>(
        identity, scheduler, serializer, packetBroker, keyValue, connection, pubSubConnection);
  }

  public static <S extends Serializable, P extends Packet> RiftClient<S, P> create(
      final Serializer serializer, final Scheduler scheduler, final RedisClient redisClient) {
    return create(String.valueOf(current().pid()), serializer, scheduler, redisClient);
  }

  public static <S extends Serializable, P extends Packet> RiftClient<S, P> create(
      final String identity,
      final Serializer serializer,
      final Scheduler scheduler,
      final RedisClient redisClient) {
    final StatefulRedisConnection<String, String> connection = redisClient.connect();
    final StatefulRedisPubSubConnection<String, String> pubSubConnection =
        redisClient.connectPubSub();

    final PacketBroker<P> packetBroker =
        RedisPacketBroker.create(identity, serializer, connection, pubSubConnection);
    final RedisKeyValue keyValue = RedisKeyValue.create(connection);

    return create(
        identity, scheduler, serializer, packetBroker, keyValue, connection, pubSubConnection);
  }

  @Override
  public void publish(final @NotNull String channelName, final @NotNull P packet) {
    packetBroker.publish(channelName, packet);
  }

  @Override
  public <R extends P> @NotNull CompletableFuture<R> request(
      final @NotNull String channelName, final @NotNull P request) {
    return packetBroker.request(channelName, request);
  }

  @Override
  public void subscribe(final @NotNull PacketSubscriber packetSubscriber) {
    packetBroker.subscribe(packetSubscriber);
  }

  @Override
  public <F, V extends S> @NotNull RiftMap<S, F, V> getMap(final @NotNull String key) {
    return RedisMap.create(key, serializer, connection);
  }

  @Override
  public @NotNull DistributedLock getLock(final @NotNull String key, final int tries) {
    return getLock(key, DEFAULT_LOCK_ACQUIRE_DELAY, DEFAULT_LOCK_ACQUIRE_UNTIL, tries);
  }

  @Override
  public @NotNull DistributedLock getLock(
      final String key, final Duration delay, final Duration until, final int tries) {
    return RedisLock.create(scheduler, key, identity, delay, until, tries, keyValue);
  }

  @Override
  public DistributedLock getLock(final @NotNull String key) {
    return getLock(key, -1);
  }

  @Override
  public void close() throws IOException {
    packetBroker.close();
    connection.close();
    pubSubConnection.close();
  }
}
