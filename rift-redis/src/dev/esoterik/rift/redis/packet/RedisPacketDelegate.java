package dev.esoterik.rift.redis.packet;

import io.lettuce.core.pubsub.RedisPubSubListener;
import java.util.function.Consumer;

final class RedisPacketDelegate implements RedisPubSubListener<String, String> {

    private final String subscribedTopic;
    private final Consumer<String> messageConsumer;

    RedisPacketDelegate(String subscribedTopic, Consumer<String> messageConsumer) {
        this.subscribedTopic = subscribedTopic;
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void message(String channelName, String message) {
        if (subscribedTopic.equals(channelName)) {
            messageConsumer.accept(message);
        }
    }

    @Override
    public void message(String pattern, String channelName, String message) {
        message("%s:%s".formatted(pattern, channelName), message);
    }

    @Override
    public void subscribed(String channel, long count) {}

    @Override
    public void psubscribed(String pattern, long count) {}

    @Override
    public void unsubscribed(String channel, long count) {}

    @Override
    public void punsubscribed(String pattern, long count) {}
}
