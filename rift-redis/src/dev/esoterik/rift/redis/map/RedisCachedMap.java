package dev.esoterik.rift.redis.map;

import dev.esoterik.rift.cache.CacheProvider;
import dev.esoterik.rift.codec.Serializer;
import dev.esoterik.rift.map.CachedMap;
import dev.esoterik.rift.map.CachedMapUpdate;
import dev.esoterik.rift.map.RiftMap;
import dev.esoterik.rift.packet.PacketBroker;
import dev.esoterik.rift.packet.PacketHandler;
import dev.esoterik.rift.packet.PacketSubscriber;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class RedisCachedMap<S extends Serializable, P extends CachedMapUpdate, F, V extends S>
        implements PacketSubscriber, CachedMap<S, P, F, V> {

    private final RiftMap<S, F, V> map;
    private final String key;
    private final PacketBroker packetBroker;
    private final String mapUpdatesTopic;
    private final Serializer serializer;
    private final CacheProvider<F, V> cacheProvider;
    private final BiFunction<String, String, P> updateFactory;

    private RedisCachedMap(
            String key,
            Serializer serializer,
            RiftMap<S, F, V> map,
            CacheProvider<F, V> cacheProvider,
            PacketBroker<?> packetBroker,
            BiFunction<String, String, P> updateFactory) {
        this.mapUpdatesTopic = "map-updates-" + key;
        this.key = key;
        this.packetBroker = packetBroker;
        this.map = map;
        this.serializer = serializer;
        this.cacheProvider = cacheProvider;
        this.updateFactory = updateFactory;
        packetBroker.subscribe(this);

        this.map.entries().forEach(cacheProvider::put);
    }

    public static <S extends Serializable, P extends CachedMapUpdate, F, V extends S> RedisCachedMap<S, P, F, V> create(
            String key,
            Serializer serializer,
            RiftMap<S, F, V> map,
            CacheProvider<F, V> cacheProvider,
            PacketBroker<?> packetBroker,
            BiFunction<String, String, P> updateFactory) {
        return new RedisCachedMap<>(key, serializer, map, cacheProvider, packetBroker, updateFactory);
    }

    @Override
    public void set(F field, V value) {
        cacheProvider.put(field, value);
        map.set(field, value);
        packetBroker.publish(
                mapUpdatesTopic, updateFactory.apply(serializer.serializeRaw(field), serializer.serialize(value)));
    }

    @Override
    public V get(F field) {
        if (field == null) {
            return null;
        }

        return cacheProvider.get(field);
    }

    @Override
    public void del(F field) {
        map.del(field);
        cacheProvider.remove(field);
        packetBroker.publish(mapUpdatesTopic, updateFactory.apply(serializer.serializeRaw(field), null));
    }

    @Override
    public Iterable<F> keys() {
        return cacheProvider.keys();
    }

    @Override
    public Stream<V> values() {
        return map.values();
    }

    @Override
    public long size() {
        return map.size();
    }

    @Override
    public String topic() {
        return mapUpdatesTopic;
    }

    @PacketHandler
    public void onUpdate(P packet) {
        if (Objects.equals(packet.source(), packetBroker.getIdentity())) {
            return;
        }

        String updateKey = packet.getKey();
        String updateValue = packet.getValue();
        if (updateValue == null) {
            cacheProvider.remove(serializer.deserializeRaw(updateKey));
            return;
        }

        cacheProvider.put(serializer.deserializeRaw(updateKey), serializer.deserialize(updateValue));
    }

    public void dump(Logger logger) {
        logger.info("RedisCachedMap: " + key);
        logger.info("Identity: " + packetBroker.getIdentity());
        logger.info("Cached Keys: " + cacheProvider.keys());
        logger.info("Global Size: " + size());
        for (F field : cacheProvider.keys()) {
            logger.info("Cached Key: " + field + ", Cached Value: " + get(field));
        }
    }
}
