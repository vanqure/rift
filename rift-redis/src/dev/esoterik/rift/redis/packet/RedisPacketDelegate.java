package dev.esoterik.rift.redis.packet;

import io.lettuce.core.pubsub.RedisPubSubListener;
import java.util.function.Consumer;

final class RedisPacketDelegate implements RedisPubSubListener<String, String> {

  private final String subscribedTopic;
  private final Consumer<String> messageConsumer;

  RedisPacketDelegate(final String subscribedTopic, final Consumer<String> messageConsumer) {
    this.subscribedTopic = subscribedTopic;
    this.messageConsumer = messageConsumer;
  }

  @Override
  public void message(final String channelName, final String message) {
    if (subscribedTopic.equals(channelName)) {
      messageConsumer.accept(message);
    }
  }

  @Override
  public void message(final String pattern, final String channelName, final String message) {
    message("%s:%s".formatted(pattern, channelName), message);
  }

  @Override
  public void subscribed(final String channel, final long count) {}

  @Override
  public void psubscribed(final String pattern, final long count) {}

  @Override
  public void unsubscribed(final String channel, final long count) {}

  @Override
  public void punsubscribed(final String pattern, final long count) {}
}
