package io.github.rift.redis.map;

import static java.util.Collections.unmodifiableMap;

import io.github.rift.codec.Serializer;
import io.github.rift.map.RiftMap;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class RedisMap<S extends Serializable, F, V extends S> implements RiftMap<S, F, V> {

    private final String key;
    private final Serializer serializer;
    private final StatefulRedisConnection<String, String> connection;

    private RedisMap(String key, Serializer serializer, StatefulRedisConnection<String, String> connection) {
        this.key = key;
        this.serializer = serializer;
        this.connection = connection;
    }

    public static <S extends Serializable, F, V extends S> RedisMap<S, F, V> create(
            String key, Serializer serializer, StatefulRedisConnection<String, String> connection) {
        return new RedisMap<>(key, serializer, connection);
    }

    @Override
    public boolean set(F field, V value) {
        if (field == null || value == null) {
            return false;
        }

        String serializedField = serializer.serializeRaw(field);
        String serializedValue = serializer.serialize(value);
        connection.sync().hset(key, serializedField, serializedValue);
        return true;
    }

    @Override
    public V get(F field) {
        if (field == null) {
            return null;
        }

        String serializedField = serializer.serializeRaw(field);
        String rawValue = connection.sync().hget(key, serializedField);
        if (rawValue == null) {
            return null;
        }
        return serializer.deserialize(rawValue);
    }

    @Override
    public boolean del(F field) {
        if (field == null) {
            return false;
        }

        String serializedField = serializer.serializeRaw(field);
        connection.sync().hdel(key, serializedField);
        return true;
    }

    @Override
    public Stream<F> fields() {
        List<String> rawFields = connection.sync().hkeys(key);
        if (rawFields == null || rawFields.isEmpty()) {
            return Stream.empty();
        }
        return rawFields.stream().map(serializer::deserializeRaw);
    }

    @Override
    public Stream<V> values() {
        List<String> rawValues = connection.sync().hvals(key);
        if (rawValues == null || rawValues.isEmpty()) {
            return Stream.empty();
        }
        return rawValues.stream().map(serializer::deserialize);
    }

    @Override
    public Map<F, V> entries() {
        Map<String, String> rawEntries = connection.sync().hgetall(key);
        if (rawEntries == null || rawEntries.isEmpty()) {
            return Map.of();
        }

        Map<F, V> entries = new HashMap<>();
        for (Map.Entry<String, String> entry : rawEntries.entrySet()) {
            String rawField = entry.getKey();
            String rawValue = entry.getValue();
            if (rawField == null || rawValue == null) {
                continue;
            }

            F field = serializer.deserializeRaw(rawField);
            V value = serializer.deserialize(rawValue);
            entries.put(field, value);
        }
        return unmodifiableMap(entries);
    }

    @Override
    public long size() {
        return connection.sync().hlen(key);
    }
}
