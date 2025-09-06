package io.github.rift.demo;

import io.github.rift.packet.PacketSubscribe;
import io.github.rift.packet.PacketSubscriber;
import java.util.concurrent.atomic.AtomicBoolean;

public record ExampleSubscriber(String topic, AtomicBoolean received) implements PacketSubscriber {

    @PacketSubscribe
    public void handle(ExamplePacket packet) {
        received.set(true);
        System.out.println("Received packet: " + packet);
    }

    @PacketSubscribe
    public ExampleResponse handle(ExampleRequest request) {
        if (request.getPlayerId() == null) {
            return null; // condition not met, do not process, just remember to handle future's throwable
            // from a timeout!
        }
        return (ExampleResponse) new ExampleResponse(request.getPlayerId() + "'s location is 0 3 3").pointAt(request);
    }
}
