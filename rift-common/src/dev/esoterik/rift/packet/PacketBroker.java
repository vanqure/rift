package dev.esoterik.rift.packet;

import dev.esoterik.rift.codec.Packet;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface PacketBroker<P extends Packet> extends Closeable {

    String getIdentity();

    void subscribe(PacketSubscriber packetSubscriber);

    void publish(String channelName, P packet);

    <R extends P> CompletableFuture<R> request(String channelName, P request);
}
