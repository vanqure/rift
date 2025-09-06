package io.github.rift;

import io.github.rift.cache.CacheProvider;
import io.github.rift.codec.Packet;
import io.github.rift.lock.DistributedLock;
import io.github.rift.map.CachedMap;
import io.github.rift.map.CachedMapUpdate;
import io.github.rift.map.RiftMap;
import io.github.rift.packet.PacketSubscriber;
import java.io.Closeable;
import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

public interface RiftClient<S extends Serializable, P extends Packet> extends Closeable {

    default DistributedLock getLock(@NotNull String key) {
        return getLock(key, -1);
    }

    void publish(@NotNull String channelName, @NotNull P packet);

    <R extends P> @NotNull CompletableFuture<R> request(@NotNull String channelName, @NotNull P request);

    void subscribe(@NotNull PacketSubscriber packetSubscriber);

    <F, V extends S> @NotNull RiftMap<S, F, V> getMap(@NotNull String key);

    <U extends CachedMapUpdate, F, V extends S> CachedMap<S, U, F, V> getCachedMap(
            String key, CacheProvider<F, V> cacheProvider, BiFunction<String, String, U> updateFactory);

    @NotNull
    DistributedLock getLock(@NotNull String key, int tries);

    @NotNull
    DistributedLock getLock(String key, Duration delay, Duration until, int tries);
}
