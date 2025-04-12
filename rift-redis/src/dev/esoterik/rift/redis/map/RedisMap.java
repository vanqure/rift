package dev.esoterik.rift.redis.map;

import static java.util.Collections.unmodifiableMap;

import dev.esoterik.rift.codec.Serializer;
import dev.esoterik.rift.map.RiftMap;
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

  private RedisMap(
      final String key,
      final Serializer serializer,
      final StatefulRedisConnection<String, String> connection) {
    this.key = key;
    this.serializer = serializer;
    this.connection = connection;
  }

  public static <S extends Serializable, F, V extends S> RedisMap<S, F, V> create(
      final String key,
      final Serializer serializer,
      final StatefulRedisConnection<String, String> connection) {
    return new RedisMap<>(key, serializer, connection);
  }

  @Override
  public boolean set(final F field, final V value) {
    if (field == null || value == null) {
      return false;
    }

    final String serializedField = serializer.serializeRaw(field);
    final String serializedValue = serializer.serialize(value);
    connection.sync().hset(key, serializedField, serializedValue);
    return true;
  }

  @Override
  public V get(final F field) {
    if (field == null) {
      return null;
    }

    final String serializedField = serializer.serializeRaw(field);
    final String rawValue = connection.sync().hget(key, serializedField);
    if (rawValue == null) {
      return null;
    }
    return serializer.deserialize(rawValue);
  }

  @Override
  public boolean del(final F field) {
    if (field == null) {
      return false;
    }

    final String serializedField = serializer.serializeRaw(field);
    connection.sync().hdel(key, serializedField);
    return true;
  }

  @Override
  public Stream<F> fields() {
    final List<String> rawFields = connection.sync().hkeys(key);
    if (rawFields == null || rawFields.isEmpty()) {
      return Stream.empty();
    }
    return rawFields.stream().map(serializer::deserializeRaw);
  }

  @Override
  public Stream<V> values() {
    final List<String> rawValues = connection.sync().hvals(key);
    if (rawValues == null || rawValues.isEmpty()) {
      return Stream.empty();
    }
    return rawValues.stream().map(serializer::deserialize);
  }

  @Override
  public Map<F, V> entries() {
    final Map<String, String> rawEntries = connection.sync().hgetall(key);
    if (rawEntries == null || rawEntries.isEmpty()) {
      return Map.of();
    }

    final Map<F, V> entries = new HashMap<>();
    for (final Map.Entry<String, String> entry : rawEntries.entrySet()) {
      final String rawField = entry.getKey();
      final String rawValue = entry.getValue();
      if (rawField == null || rawValue == null) {
        continue;
      }

      final F field = serializer.deserializeRaw(rawField);
      final V value = serializer.deserialize(rawValue);
      entries.put(field, value);
    }
    return unmodifiableMap(entries);
  }

  @Override
  public long size() {
    return connection.sync().hlen(key);
  }
}
