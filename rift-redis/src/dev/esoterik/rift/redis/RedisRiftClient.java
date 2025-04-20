package dev.esoterik.rift.redis;

import static java.lang.ProcessHandle.current;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import dev.esoterik.rift.RiftClient;
import dev.esoterik.rift.cache.CacheProvider;
import dev.esoterik.rift.codec.Packet;
import dev.esoterik.rift.codec.Serializer;
import dev.esoterik.rift.lock.DistributedLock;
import dev.esoterik.rift.map.CachedMap;
import dev.esoterik.rift.map.CachedMapUpdate;
import dev.esoterik.rift.map.RiftMap;
import dev.esoterik.rift.packet.PacketBroker;
import dev.esoterik.rift.packet.PacketSubscriber;
import dev.esoterik.rift.redis.lock.RedisLock;
import dev.esoterik.rift.redis.map.RedisCachedMap;
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
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

public final class RedisRiftClient<S extends Serializable, P extends Packet> implements RiftClient<S, P> {

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
            String identity,
            Scheduler scheduler,
            Serializer serializer,
            PacketBroker<P> packetBroker,
            RedisKeyValue keyValue,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection) {
        this.identity = identity;
        this.scheduler = scheduler;
        this.serializer = serializer;
        this.packetBroker = packetBroker;
        this.keyValue = keyValue;
        this.connection = connection;
        this.pubSubConnection = pubSubConnection;
    }

    public static <S extends Serializable, P extends Packet> RiftClient<S, P> create(
            String identity,
            Scheduler scheduler,
            Serializer serializer,
            PacketBroker<P> packetBroker,
            RedisKeyValue keyValue,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection) {
        return new RedisRiftClient<>(
                identity, scheduler, serializer, packetBroker, keyValue, connection, pubSubConnection);
    }

    public static <S extends Serializable, P extends Packet> RiftClient<S, P> create(
            Serializer serializer, Scheduler scheduler, RedisClient redisClient) {
        return create(String.valueOf(current().pid()), serializer, scheduler, redisClient);
    }

    public static <S extends Serializable, P extends Packet> RiftClient<S, P> create(
            String identity, Serializer serializer, Scheduler scheduler, RedisClient redisClient) {
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub();

        PacketBroker<P> packetBroker = RedisPacketBroker.create(identity, serializer, connection, pubSubConnection);
        RedisKeyValue keyValue = RedisKeyValue.create(connection);

        return create(identity, scheduler, serializer, packetBroker, keyValue, connection, pubSubConnection);
    }

    @Override
    public void publish(@NotNull String channelName, @NotNull P packet) {
        packetBroker.publish(channelName, packet);
    }

    @Override
    public <R extends P> @NotNull CompletableFuture<R> request(@NotNull String channelName, @NotNull P request) {
        return packetBroker.request(channelName, request);
    }

    @Override
    public void subscribe(@NotNull PacketSubscriber packetSubscriber) {
        packetBroker.subscribe(packetSubscriber);
    }

    @Override
    public <F, V extends S> @NotNull RiftMap<S, F, V> getMap(@NotNull String key) {
        return RedisMap.create(key, serializer, connection);
    }

    @Override
    public <U extends CachedMapUpdate, F, V extends S> CachedMap<S, U, F, V> getCachedMap(
            String key, CacheProvider<F, V> cacheProvider, BiFunction<String, String, U> updateFactory) {
        return RedisCachedMap.create(key, serializer, getMap(key), cacheProvider, packetBroker, updateFactory);
    }

    @Override
    public @NotNull DistributedLock getLock(@NotNull String key, int tries) {
        return getLock(key, DEFAULT_LOCK_ACQUIRE_DELAY, DEFAULT_LOCK_ACQUIRE_UNTIL, tries);
    }

    @Override
    public @NotNull DistributedLock getLock(String key, Duration delay, Duration until, int tries) {
        return RedisLock.create(scheduler, key, identity, delay, until, tries, keyValue);
    }

    @Override
    public DistributedLock getLock(@NotNull String key) {
        return getLock(key, -1);
    }

    @Override
    public void close() throws IOException {
        packetBroker.close();
        connection.close();
        pubSubConnection.close();
    }
}
