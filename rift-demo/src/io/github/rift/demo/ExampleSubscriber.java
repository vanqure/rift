package io.github.rift.demo;

import io.github.wisp.subscription.Subscribe;
import io.github.wisp.subscription.Subscriber;
import java.util.concurrent.atomic.AtomicBoolean;

public record ExampleSubscriber(String topic, AtomicBoolean received) implements Subscriber {

    @Subscribe
    public void handle(ExamplePacket packet) {
        received.set(true);
        System.out.println("Received packet: " + packet);
    }

    @Subscribe
    public ExampleResponse handle(ExampleRequest request) {
        if (request.getPlayerId() == null) {
            return null; // condition not met, do not process, just remember to handle future's throwable
            // from a timeout!
        }
        return new ExampleResponse(request.getPlayerId() + "'s location is 0 3 3");
    }
}
