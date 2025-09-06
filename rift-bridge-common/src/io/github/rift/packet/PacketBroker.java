package io.github.rift.packet;

import io.github.rift.serializer.Packet;
import io.github.wisp.subscription.Subscriber;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface PacketBroker<P extends Packet> extends Closeable {

    String identity();

    void publish(String topic, P packet);

    <R extends P> CompletableFuture<R> request(String topic, P request);

    void subscribe(Subscriber subscriber);
}
