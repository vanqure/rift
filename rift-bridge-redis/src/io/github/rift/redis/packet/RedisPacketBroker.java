package io.github.rift.redis.packet;

import io.github.rift.serializer.Packet;
import io.github.rift.serializer.Serializer;
import io.github.rift.packet.PacketBroker;
import io.github.wisp.Wisp;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.time.Duration;

public interface RedisPacketBroker<P extends Packet> extends PacketBroker<P> {

    static <P extends Packet> RedisPacketBroker<P> create(
            Wisp wisp,
            String identity,
            Serializer serializer,
            Duration requestCleanupInterval,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection) {
        return new RedisPacketBrokerImpl<>(
                wisp,
                identity,
                serializer,
                requestCleanupInterval,
                connection,
                pubSubConnection);
    }

    static <P extends Packet> RedisPacketBroker<P> create(
            String identity,
            Serializer serializer,
            Duration requestCleanupInterval,
            StatefulRedisConnection<String, String> connection,
            StatefulRedisPubSubConnection<String, String> pubSubConnection) {
        return create(Wisp.create(), identity, serializer, requestCleanupInterval, connection, pubSubConnection);
    }
}
