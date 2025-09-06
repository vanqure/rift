package io.github.rift.redis.map;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

public final class RedisKeyValue implements KeyValue {

    private final StatefulRedisConnection<String, String> connection;

    private RedisKeyValue(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    public static RedisKeyValue create(@NotNull StatefulRedisConnection<String, String> connection) {
        return new RedisKeyValue(connection);
    }

    @Override
    public boolean set(String key, String value) {
        return "OK".equals(connection.sync().set(key, value));
    }

    @Override
    public boolean set(String key, String value, Duration ttl) {
        SetArgs setArgs = new SetArgs();
        long ttlToMillis = ttl.toMillis();
        if (ttlToMillis > 0) {
            setArgs.px(ttlToMillis);
        }
        return "OK".equals(connection.sync().set(key, value, setArgs));
    }

    @Override
    public boolean set(String key, String value, Duration ttl, boolean onlyIfNotExists) {
        SetArgs setArgs = new SetArgs();

        long ttlToMillis = ttl.toMillis();
        if (ttlToMillis > 0) {
            setArgs.px(ttlToMillis);
        }

        if (onlyIfNotExists) {
            setArgs.nx();
        }
        return "OK".equals(connection.sync().set(key, value, setArgs));
    }

    @Override
    public String get(String key) {
        return connection.sync().get(key);
    }

    @Override
    public boolean del(String key) {
        return connection.sync().del(key) > 0;
    }

    @Override
    public boolean ttl(String key, Instant expireAt) {
        return connection.sync().expireat(key, expireAt);
    }

    @Override
    public long ttl(String key) {
        return connection.sync().ttl(key);
    }

    @Override
    public long increment(String key, long amount) {
        return connection.sync().incrby(key, amount);
    }

    @Override
    public long decrement(String key, long amount) {
        return connection.sync().decrby(key, amount);
    }

    @Override
    public boolean contains(String key) {
        return connection.sync().exists(key) > 0;
    }
}
